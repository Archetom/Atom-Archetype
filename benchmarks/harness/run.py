#!/usr/bin/env python3
"""Offline, model-neutral benchmark orchestration. Python standard library only."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import platform
import re
import runpy
import shutil
import subprocess
import sys
import tarfile
import time
import uuid
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
BENCHMARKS = HERE.parent
REPOSITORY = BENCHMARKS.parent
SCENARIOS = ("add-api", "domain-invariant", "persistence-change")
UNAVAILABLE = "unavailable"
IGNORED = {"target", ".git", "__pycache__"}
COMMON_CLASS = "com.example.benchmark.BenchmarkArchitectureTest"


class Blocked(RuntimeError):
    pass


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    temporary.replace(path)


def read_json(path):
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"Expected a JSON object: {path}")
    return value


def files(root):
    """Do not follow candidate-controlled links, including links in ignored directories."""
    for directory, directories, names in os.walk(root, followlinks=False):
        parent = Path(directory)
        for name in directories + names:
            if (parent / name).is_symlink():
                raise ValueError(f"Symlinks are not accepted in a benchmark project: {parent / name}")
        directories[:] = sorted(name for name in directories if name not in IGNORED)
        for name in sorted(names):
            yield parent / name


def inventory(root):
    return {str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in files(root)}


def fingerprint(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()


def copy_project(source, destination):
    destination.mkdir(parents=True, exist_ok=False)
    try:
        for path in files(source):
            target = destination / path.relative_to(source)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, target)
    except BaseException:
        # Only this invocation created destination. A failed snapshot must be retryable.
        shutil.rmtree(destination)
        raise


def new_directory(path):
    path = Path(path).absolute()
    if path.exists() or path.is_symlink():
        raise ValueError(f"Output must not already exist: {path}")
    path.mkdir(parents=True)
    return path.resolve()


def checked_child(root, relative):
    path = root / relative
    if Path(relative).is_absolute() or ".." in Path(relative).parts or path.is_symlink():
        raise ValueError(f"Invalid relative artifact path: {relative}")
    if not path.resolve().is_relative_to(root.resolve()):
        raise ValueError(f"Artifact escapes its root: {relative}")
    return path


def execute(command, cwd, log, env=None):
    log.parent.mkdir(parents=True, exist_ok=True)
    started = time.monotonic()
    with log.open("wb") as output:
        try:
            result = subprocess.run(command, cwd=cwd, env=env, stdout=output, stderr=subprocess.STDOUT)
        except FileNotFoundError as error:
            raise Blocked(f"Required executable unavailable: {command[0]}") from error
    return {"exitCode": result.returncode, "durationMs": round((time.monotonic() - started) * 1000),
            "logPath": str(log.resolve())}


def java_version():
    java = str(Path(os.environ["JAVA_HOME"]) / "bin/java") if os.environ.get("JAVA_HOME") else "java"
    try:
        result = subprocess.run([java, "-version"], capture_output=True, text=True)
    except FileNotFoundError as error:
        raise Blocked("JDK is unavailable; select the JDK required by the POM") from error
    if result.returncode:
        raise Blocked("The selected JDK cannot run")
    return (result.stderr or result.stdout).splitlines()[0]


def execution_environment():
    return {"java": java_version(), "os": platform.platform(), "python": platform.python_version()}


def build_environment(root, integration=False):
    env = os.environ.copy()
    env["CI"] = "true" if integration else "false"
    env["MAVEN_USER_HOME"] = str(root / "maven-home")
    # Do not let external CLI arguments silently skip acceptance tests.
    env.pop("MAVEN_ARGS", None)
    return env


def maven(root, *arguments):
    return ["sh", "./mvnw", "-B", "-ntp", f"-Dmaven.repo.local={root / 'repository'}", *arguments]


def seed(args):
    """Generate once from an immutable Git commit; all later runs copy its clean sources."""
    revision = subprocess.check_output(["git", "rev-parse", "--verify", args.revision + "^{commit}"],
                                       cwd=REPOSITORY, text=True).strip()
    root = new_directory(args.output)
    state = {"schemaVersion": 1, "kind": "seed", "revision": revision, "status": "not-run"}
    write_json(root / "seed.json", state)
    try:
        source = root / "source"
        source.mkdir()
        archive = subprocess.check_output(["git", "archive", "--format=tar", revision], cwd=REPOSITORY)
        with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
            for member in tar.getmembers():
                checked_child(source, member.name)
                if not (member.isfile() or member.isdir()):
                    raise ValueError("Source archive may contain only regular files and directories")
            tar.extractall(source)
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        pom = ET.parse(source / "pom.xml").getroot()
        prop = lambda name: pom.findtext(f"m:properties/m:{name}", namespaces=ns)
        selected_java = java_version()
        major = re.search(r'"(\d+)', selected_java)
        if not major or int(major[1]) < int(prop("java.version")):
            raise Blocked(f"JDK {prop('java.version')} or newer required; selected {selected_java}")
        state["environment"] = {"java": selected_java, "os": platform.platform(),
                                "python": platform.python_version()}
        if args.cache_from:
            shutil.copytree(Path(args.cache_from).resolve(), root / "repository",
                            ignore=shutil.ignore_patterns(".locks", "*.lastUpdated"))
        wrapper = Path(os.environ.get("MAVEN_USER_HOME", str(Path.home() / ".m2"))) / "wrapper"
        if wrapper.is_dir():
            shutil.copytree(wrapper, root / "maven-home/wrapper")
        env = build_environment(root)
        installed = execute(maven(root, "clean", "install", "-Dgpg.skip=true"), source,
                            root / "logs/install.log", env)
        state["install"] = installed
        if installed["exitCode"]:
            raise RuntimeError("Pinned archetype build failed; inspect install.log")
        version = pom.findtext("m:version", namespaces=ns)
        generated = execute(maven(root,
            f"org.apache.maven.plugins:maven-archetype-plugin:{prop('maven.archetype.packaging.version')}:generate",
            "-DarchetypeGroupId=io.github.archetom", "-DarchetypeArtifactId=atom-archetype",
            f"-DarchetypeVersion={version}", "-DgroupId=com.example.benchmark",
            "-DartifactId=benchmark-app", "-Dpackage=com.example.benchmark", "-Dversion=1.0.0-SNAPSHOT",
            f"-DoutputDirectory={root / 'generated'}", "-DinteractiveMode=false"),
            source, root / "logs/generate.log", env)
        state["generate"] = generated
        if generated["exitCode"]:
            raise RuntimeError("Pinned application generation failed; inspect generate.log")
        copy_project(root / "generated/benchmark-app", root / "project")
        state["files"] = inventory(root / "project")
        state["fingerprint"] = fingerprint(state["files"])
        state["status"] = "passed"
    except (Blocked, OSError, RuntimeError, ValueError) as error:
        state["status"] = "blocked" if isinstance(error, Blocked) else "failed"
        state["reason"] = str(error)
    write_json(root / "seed.json", state)
    return state


def verified_seed(path):
    root = Path(path).resolve()
    data = read_json(root / "seed.json")
    if data.get("kind") != "seed" or data.get("status") != "passed":
        raise Blocked("Seed generation has not passed")
    if inventory(root / "project") != data["files"]:
        raise ValueError("Seed was modified; create a new seed from the pinned commit")
    return root, data


def treatment(project, mode):
    if mode == "baseline":
        shutil.rmtree(project / ".agents/skills", ignore_errors=True)
        for path in list(project.rglob("*.md")) + [project / "llms.txt"]:
            if not path.is_file():
                continue
            content = path.read_text(encoding="utf-8")
            content = re.sub(r"(?ms)^## Task workflows\n.*?(?=^## |\Z)", "", content)
            content = re.sub(r"\[[^\]]+\]\([^)]*(?:\.agents/skills|AGENTS.md#task-workflows)[^)]*\)",
                             "the application guide", content)
            path.write_text(content, encoding="utf-8")


def prepare(args):
    seed_root, seed_data = verified_seed(args.seed)
    definition = BENCHMARKS / "scenarios" / args.scenario
    scenario = read_json(definition / "scenario.json")
    root = new_directory(args.output)
    copy_project(seed_root / "project", root / "workspace")
    treatment(root / "workspace", args.mode)
    shutil.copyfile(definition / "task.md", root / "workspace/TASK.md")
    if args.hints:
        shutil.copyfile(definition / "hints.md", root / "workspace/HINTS.md")
    git = ["git", "-c", "user.name=Benchmark Harness", "-c", "user.email=benchmark@example.invalid",
           "-c", "commit.gpgsign=false", "-c", "core.hooksPath=/dev/null"]
    for index, command in enumerate((["init", "--template="], ["add", "."], ["commit", "-m", "Benchmark starting state"])):
        process = execute(git + command, root / "workspace", root / f"logs/git-{index}.log")
        if process["exitCode"]:
            raise Blocked("Could not initialize the disposable project's Git history")
    # Keep an evaluator-owned snapshot of the rubric outside the agent workspace.
    shutil.copytree(HERE / "acceptance", root / "acceptance")
    state = {"schemaVersion": 1, "kind": "run", "runId": str(uuid.uuid4()), "status": "not-run",
             "seed": str(seed_root), "revision": seed_data["revision"], "seedFingerprint": seed_data["fingerprint"],
             "scenario": scenario, "mode": args.mode, "hintsProvided": args.hints,
             "rubricFingerprint": fingerprint({"scenario": scenario, "acceptance": inventory(root / "acceptance")}),
             "taskFingerprint": fingerprint({"task": (definition / "task.md").read_text(encoding="utf-8"),
                                              "hints": (definition / "hints.md").read_text(encoding="utf-8") if args.hints else None}),
             "createdAt": time.time(), "model": args.model or UNAVAILABLE,
             "modelVersion": args.model_version or UNAVAILABLE,
             "agentEnvironment": getattr(args, "environment_id", None) or UNAVAILABLE,
             "environment": execution_environment(), "seedEnvironment": seed_data.get("environment", {}),
             "humanInterventions": UNAVAILABLE, "tokenUsage": UNAVAILABLE,
             "validationAttempts": 0, "submissions": [], "evaluations": [],
             "workspaceFiles": inventory(root / "workspace")}
    write_json(root / "run.json", state)
    summarize(root, state)
    return {"status": "not-run", "runPath": str(root), "workspace": str(root / "workspace"),
            "revision": state["revision"], "mode": state["mode"]}


def load_run(path):
    root = Path(path).resolve()
    state = read_json(root / "run.json")
    if state.get("kind") != "run":
        raise ValueError("Not a benchmark run")
    seed_root, seed_data = verified_seed(state["seed"])
    if seed_data["fingerprint"] != state["seedFingerprint"] or seed_data["revision"] != state["revision"]:
        raise ValueError("Run's pinned seed no longer matches")
    if fingerprint({"scenario": state["scenario"], "acceptance": inventory(root / "acceptance")}) != state["rubricFingerprint"]:
        raise ValueError("Evaluator acceptance tests were modified")
    return root, state, seed_root


def submit(args):
    root, state, _ = load_run(args.run)
    number = len(state["submissions"]) + 1
    destination = root / "submissions" / f"{number:04d}"
    copy_project(root / "workspace", destination)
    current = inventory(destination)
    changed = sorted(name for name in state["workspaceFiles"].keys() | current.keys()
                     if state["workspaceFiles"].get(name) != current.get(name))
    state["submissions"].append({"number": number, "submittedAt": time.time(),
                                 "files": current, "filesChanged": changed})
    state["status"] = "not-run"
    write_json(root / "run.json", state)
    summarize(root, state)
    return {"status": "not-run", "submission": number, "filesChanged": len(changed)}


def protected_changes(seed_files, candidate_files):
    def protected(name):
        path = Path(name)
        return (path.name == "pom.xml" or name in {"mvnw", "mvnw.cmd", "Makefile", "clean.sh"}
                or name.startswith((".mvn/", "scripts/"))
                or ("/db/migration/" in name and name in seed_files))
    return sorted(name for name in seed_files.keys() | candidate_files.keys()
                  if protected(name) and seed_files.get(name) != candidate_files.get(name))


def restore_oracle(seed_project, candidate):
    # Keep new candidate tests; restore all original tests to prevent weakening the oracle.
    for source in files(seed_project):
        name = str(source.relative_to(seed_project))
        if "/src/test/" in name:
            target = candidate / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)


def parse_reports(project):
    cases = {}
    paths = sorted(project.glob("**/target/surefire-reports/TEST-*.xml"))
    paths += sorted(project.glob("**/target/failsafe-reports/TEST-*.xml"))
    for path in paths:
        suite = ET.parse(path).getroot()
        for case in suite.iter("testcase"):
            key = (case.get("classname", ""), case.get("name", ""))
            status = ("skipped" if case.find("skipped") is not None else
                      "error" if case.find("error") is not None else
                      "failed" if case.find("failure") is not None else "passed")
            if key in cases:
                raise ValueError(f"Duplicate test result: {key}")
            cases[key] = status
    return cases, [str(path.resolve()) for path in paths]


def assess(project, scenario, exit_code):
    cases, report_paths = parse_reports(project)
    expected = [(entry["className"], method["name"], method["category"])
                for entry in scenario["acceptance"] for method in entry["methods"]]
    expected += [(COMMON_CLASS, "recordsActualArchitectureViolations", "architecture"),
                 ("com.example.benchmark.ArchitectureBoundaryTest", "compiledArchitectureRespectsBoundaries", "architecture")]
    outcomes = [{"className": cls, "method": method, "category": category,
                 "status": cases.get((cls, method), "not-run")} for cls, method, category in expected]
    security = [item for item in outcomes if item["category"] == "security"]
    security_count = (sum(item["status"] == "failed" for item in security)
                      if security and all(item["status"] in {"passed", "failed"} for item in security) else UNAVAILABLE)
    architecture_count = UNAVAILABLE
    diagnostic = project / "start/target/benchmark-architecture.json"
    if diagnostic.is_file():
        value = read_json(diagnostic).get("violationCount")
        if isinstance(value, int) and not isinstance(value, bool) and value >= 0:
            architecture_count = value
    successful = (exit_code == 0 and bool(report_paths) and architecture_count == 0
                  and all(item["status"] == "passed" for item in outcomes)
                  and not any(value in {"failed", "error"} for value in cases.values()))
    return {"status": "passed" if successful else "failed", "taskSuccess": successful,
            "architectureViolations": architecture_count, "securityViolations": security_count,
            "testCoverage": {"definition": "independent required behavior checks, not line coverage",
                             "passed": sum(item["status"] == "passed" for item in outcomes), "total": len(outcomes)},
            "acceptance": outcomes, "reportPaths": report_paths}


def evaluate(args):
    root, state, seed_root = load_run(args.run)
    if not state["submissions"]:
        raise ValueError("Submit a complete candidate before evaluation")
    submission = state["submissions"][-1]
    snapshot = root / "submissions" / f"{submission['number']:04d}"
    if inventory(snapshot) != submission["files"]:
        raise ValueError("Submitted snapshot was modified; submit a new candidate")
    number = len(state["evaluations"]) + 1
    evaluation = root / "evaluations" / f"{number:04d}"
    evaluation.mkdir(parents=True)
    result = {"status": "not-run", "submission": submission["number"], "attempt": number,
              "taskSuccess": UNAVAILABLE, "architectureViolations": UNAVAILABLE,
              "securityViolations": UNAVAILABLE, "testCoverage": UNAVAILABLE}
    started = time.monotonic()
    try:
        illegal = protected_changes(read_json(seed_root / "seed.json")["files"], submission["files"])
        if illegal:
            raise ValueError("Candidate changed protected build infrastructure or applied migrations: " + ", ".join(illegal))
        current_environment = execution_environment()
        result["environment"] = current_environment
        if current_environment != state["environment"]:
            raise Blocked("Execution environment changed since preparation; restore it or prepare a new run")
        if current_environment["java"] != read_json(seed_root / "seed.json")["environment"].get("java"):
            raise Blocked("Selected JDK differs from seed; restore the recorded JDK")
        if state["scenario"]["requiresDocker"]:
            docker = execute(["docker", "info"], root, evaluation / "docker.log")
            if docker["exitCode"]:
                raise Blocked("Docker is unavailable; integration acceptance has not run")
        project = evaluation / "project"
        copy_project(snapshot, project)
        restore_oracle(seed_root / "project", project)
        runpy.run_path(str(root / "acceptance/compatibility.py"))["apply"](project, state["scenario"]["id"])
        for entry in state["scenario"]["acceptance"] + state["scenario"].get("resources", []):
            source = checked_child(root / "acceptance", entry["source"])
            destination = checked_child(project, entry["destination"])
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, destination)
        shutil.copyfile(root / "acceptance/common/BenchmarkArchitectureTest.java",
                        project / "start/src/test/java/com/example/benchmark/BenchmarkArchitectureTest.java")
        # Each run has independent writable build caches. No candidate output enters another run.
        if not (root / "repository").exists():
            shutil.copytree(seed_root / "repository", root / "repository",
                            ignore=shutil.ignore_patterns(".locks", "*.lastUpdated"))
        if not (root / "maven-home").exists():
            shutil.copytree(seed_root / "maven-home", root / "maven-home")
        process = execute(maven(root, "clean", "test"), project, evaluation / "maven.log",
                          build_environment(root, state["scenario"]["requiresDocker"]))
        result.update(process)
        result.update(assess(project, state["scenario"], process["exitCode"]))
    except (Blocked, OSError, ValueError, ET.ParseError) as error:
        result.update(status="blocked" if isinstance(error, Blocked) else "failed", reason=str(error))
        if result["status"] == "failed":
            result["taskSuccess"] = False
    result["durationMs"] = round((time.monotonic() - started) * 1000)
    write_json(evaluation / "result.json", result)
    state["evaluations"].append(result)
    state["validationAttempts"] = number
    state["status"] = result["status"]
    write_json(root / "run.json", state)
    summarize(root, state)
    return result


def summarize(root, state):
    latest = state["evaluations"][-1] if state["evaluations"] and state["status"] != "not-run" else {}
    first = next((entry for entry in state["evaluations"] if entry["submission"] == 1
                  and entry["status"] in {"passed", "failed"}), None)
    submission = state["submissions"][-1] if state["submissions"] else None
    metrics = {"taskSuccess": latest.get("taskSuccess", UNAVAILABLE),
               "firstPassSuccess": first["status"] == "passed" if first else UNAVAILABLE,
               "architectureViolations": latest.get("architectureViolations", UNAVAILABLE),
               "securityViolations": latest.get("securityViolations", UNAVAILABLE),
               "testCoverage": latest.get("testCoverage", UNAVAILABLE),
               "humanInterventions": state["humanInterventions"], "tokenUsage": state["tokenUsage"],
               "durationMs": round((submission["submittedAt"] - state["createdAt"]) * 1000) if submission else UNAVAILABLE,
               "filesChanged": len(submission["filesChanged"]) if submission else UNAVAILABLE,
               "validationAttempts": state["validationAttempts"],
               "agentLocalValidationAttempts": state.get("agentLocalValidationAttempts", UNAVAILABLE)}
    local_attempts = metrics["agentLocalValidationAttempts"]
    metrics["totalValidationAttempts"] = (state["validationAttempts"] + local_attempts
                                           if isinstance(local_attempts, int) else UNAVAILABLE)
    report = {"schemaVersion": 1, "status": state["status"], "runId": state["runId"],
              "revision": state["revision"], "seedFingerprint": state["seedFingerprint"],
              "rubricFingerprint": state["rubricFingerprint"], "scenario": state["scenario"]["id"],
              "taskFingerprint": state.get("taskFingerprint", UNAVAILABLE),
              "mode": state["mode"], "hintsProvided": state["hintsProvided"], "model": state["model"],
              "modelVersion": state["modelVersion"], "environment": state["environment"], "metrics": metrics}
    report["agentEnvironment"] = state.get("agentEnvironment", UNAVAILABLE)
    write_json(root / "result.json", report)
    lines = [f"# Benchmark: {report['scenario']}", "", f"Status: **{report['status']}**", "",
             f"Mode: `{report['mode']}` · Revision: `{report['revision']}`", "",
             "| Metric | Value |", "| --- | --- |"]
    for name, value in metrics.items():
        lines.append(f"| {name} | {json.dumps(value, ensure_ascii=False)} |")
    lines += ["", "Duration is wall time from preparation to the latest submission, including pauses.",
              "Validation attempts count evaluator runs; unobserved agent-local test attempts are not inferred.",
              "Human interventions and tokens are operator-recorded observations; unavailable values are not estimates.",
              "Both modes retain AGENTS.md; comparisons measure the incremental effect of Skills."]
    (root / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return report


def record(args):
    root, state, _ = load_run(args.run)
    if args.human_interventions is not None:
        state["humanInterventions"] = args.human_interventions
    if getattr(args, "local_validation_attempts", None) is not None:
        state["agentLocalValidationAttempts"] = args.local_validation_attempts
    if args.input_tokens is not None or args.output_tokens is not None:
        old = state["tokenUsage"] if isinstance(state["tokenUsage"], dict) else {}
        incoming = args.input_tokens if args.input_tokens is not None else old.get("input", UNAVAILABLE)
        outgoing = args.output_tokens if args.output_tokens is not None else old.get("output", UNAVAILABLE)
        state["tokenUsage"] = {"input": incoming, "output": outgoing,
                               "total": incoming + outgoing if isinstance(incoming, int) and isinstance(outgoing, int) else UNAVAILABLE}
    write_json(root / "run.json", state)
    return summarize(root, state)


def compare(args):
    reports = [read_json(Path(path).resolve() / "result.json") for path in args.runs]
    controls = ["revision", "seedFingerprint", "rubricFingerprint", "taskFingerprint", "scenario", "hintsProvided", "model", "modelVersion", "environment", "agentEnvironment"]
    differences = [key for key in controls if any(report.get(key, UNAVAILABLE) != reports[0].get(key, UNAVAILABLE) for report in reports[1:])]
    missing = [key for key in ("model", "modelVersion", "taskFingerprint", "agentEnvironment") if any(report.get(key, UNAVAILABLE) == UNAVAILABLE for report in reports)]
    complete = all(report["status"] in {"passed", "failed"} for report in reports)
    paired = {report["mode"] for report in reports} == {"baseline", "engineering"}
    return {"status": "passed" if not differences and not missing and complete and paired else "blocked",
            "comparable": not differences and not missing and complete and paired,
            "differentControls": differences, "unavailableControls": missing,
            "completedRuns": complete, "bothModesPresent": paired,
            "interpretation": "Skills increment only; no causal benefit is established by a single pair", "runs": reports}


def self_test(args):
    # These fixtures calibrate the evaluator, not a model's ability or cost.
    import reference_solutions
    root = new_directory(args.output)
    results = []
    for scenario in SCENARIOS:
        definition = read_json(BENCHMARKS / "scenarios" / scenario / "scenario.json")
        for variant in ("correct", "mutant"):
            run = root / f"{scenario}-{variant}"
            prepare(argparse.Namespace(seed=args.seed, output=run, scenario=scenario, mode="engineering",
                                       hints=False, model="evaluator-calibration", model_version="reference-fixture-v1",
                                       environment_id="local-reference-calibration"))
            reference_solutions.apply(run / "workspace", scenario, variant)
            submit(argparse.Namespace(run=run))
            result = evaluate(argparse.Namespace(run=run))
            rejected = {item["method"] for item in result.get("acceptance", []) if item["status"] == "failed"}
            matched = (result["status"] == "passed" if variant == "correct" else
                       result["status"] == "failed" and bool(rejected.intersection(definition["mutantRejectedBy"])))
            results.append({"scenario": scenario, "variant": variant, "status": result["status"],
                            "expectedOutcomeObserved": matched, "resultPath": str(run / "result.json")})
            print(f"[benchmark calibration] {scenario}/{variant}: {result['status']}", file=sys.stderr)
    outcome = {"status": "passed" if all(item["expectedOutcomeObserved"] for item in results) else
               "blocked" if any(item["status"] == "blocked" for item in results) else "failed", "calibration": results}
    write_json(root / "calibration.json", outcome)
    return outcome


def nonnegative(value):
    number = int(value)
    if number < 0:
        raise argparse.ArgumentTypeError("Must be nonnegative")
    return number


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    p = commands.add_parser("seed")
    p.add_argument("--revision", default="HEAD")
    p.add_argument("--output", required=True)
    p.add_argument("--cache-from", help="Optional Maven artifact cache to copy, never link or modify")
    p = commands.add_parser("prepare")
    p.add_argument("--seed", required=True)
    p.add_argument("--output", required=True)
    p.add_argument("--scenario", choices=SCENARIOS, required=True)
    p.add_argument("--mode", choices=("baseline", "engineering"), required=True)
    p.add_argument("--hints", action="store_true")
    p.add_argument("--model")
    p.add_argument("--model-version")
    p.add_argument("--environment-id", help="Operator's exact agent runner/settings/resource profile; omit if unknown")
    for name in ("submit", "evaluate", "summary", "record"):
        p = commands.add_parser(name)
        p.add_argument("--run", required=True)
        if name == "record":
            p.add_argument("--human-interventions", type=nonnegative)
            p.add_argument("--local-validation-attempts", type=nonnegative)
            p.add_argument("--input-tokens", type=nonnegative)
            p.add_argument("--output-tokens", type=nonnegative)
    p = commands.add_parser("compare")
    p.add_argument("runs", nargs="+", help="Run directories; use identical scenario/model/environment controls")
    p = commands.add_parser("self-test")
    p.add_argument("--seed", required=True)
    p.add_argument("--output", required=True)
    args = parser.parse_args()
    try:
        if args.command == "summary":
            root, state, _ = load_run(args.run)
            result = summarize(root, state)
        else:
            result = globals()[args.command.replace("-", "_")](args)
    except (OSError, ValueError, KeyError, Blocked, subprocess.CalledProcessError) as error:
        result = {"status": "blocked" if isinstance(error, Blocked) else "failed", "reason": str(error)}
    print(json.dumps(result, ensure_ascii=False))
    return 0 if result["status"] in {"passed", "not-run"} else 2 if result["status"] == "blocked" else 1


if __name__ == "__main__":
    sys.exit(main())
