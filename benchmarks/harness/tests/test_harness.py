import argparse
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

HARNESS = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("benchmark_harness", HARNESS / "run.py")
harness = importlib.util.module_from_spec(spec)
spec.loader.exec_module(harness)


class HarnessTests(unittest.TestCase):
    def setUp(self):
        java = patch.object(harness, "java_version", return_value="fixture JDK")
        java.start()
        self.addCleanup(java.stop)
        self.temp = tempfile.TemporaryDirectory(prefix="atom-benchmark-test-")
        self.root = Path(self.temp.name)
        self.seed = self.root / "seed"
        project = self.seed / "project"
        project.mkdir(parents=True)
        content = {
            "pom.xml": "<project/>", "mvnw": "exit 0\n",
            "AGENTS.md": "# Rules\nMandatory security\n\n## Task workflows\nUse [skill](.agents/skills/test/SKILL.md)\n\n## Architecture\nDDD rules\n",
            ".agents/skills/test/SKILL.md": "A workflow",
            "llms.txt": "## Task workflows\n- [skill](.agents/skills/test/SKILL.md)\n\n## Architecture\nRules",
            "domain/src/main/java/Thing.java": "class Thing {}",
            "start/src/test/java/com/example/benchmark/BaselineTest.java": "trusted test",
            "infra/persistence/src/main/resources/db/migration/V1__create_user_table.sql": "schema",
        }
        for name, text in content.items():
            path = project / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
        (self.seed / "repository").mkdir()
        (self.seed / "maven-home").mkdir()
        values = harness.inventory(project)
        harness.write_json(self.seed / "seed.json", {"kind": "seed", "status": "passed", "revision": "a" * 40,
            "files": values, "fingerprint": harness.fingerprint(values), "environment": {"java": "fixture JDK"}})

    def tearDown(self):
        self.temp.cleanup()

    def prepare(self, name="run", mode="engineering", scenario="domain-invariant"):
        root = self.root / name
        harness.prepare(argparse.Namespace(seed=self.seed, output=root, scenario=scenario, mode=mode,
                        hints=False, model=None, model_version=None))
        return root

    def submit(self, root):
        return harness.submit(argparse.Namespace(run=root))

    def successful_reports(self, project, scenario, skip=None):
        directory = project / "start/target/surefire-reports"
        directory.mkdir(parents=True)
        suite = ET.Element("testsuite")
        expected = [(entry["className"], method["name"]) for entry in scenario["acceptance"] for method in entry["methods"]]
        expected += [(harness.COMMON_CLASS, "recordsActualArchitectureViolations"),
                     ("com.example.benchmark.ArchitectureBoundaryTest", "compiledArchitectureRespectsBoundaries")]
        for cls, name in expected:
            case = ET.SubElement(suite, "testcase", classname=cls, name=name)
            if skip == name:
                ET.SubElement(case, "skipped")
        ET.ElementTree(suite).write(directory / "TEST-fixture.xml")
        harness.write_json(project / "start/target/benchmark-architecture.json", {"violationCount": 0})

    def test_modes_share_the_same_seed_but_not_mutable_files(self):
        baseline = self.prepare("baseline", "baseline")
        engineering = self.prepare("engineering")
        self.assertFalse((baseline / "workspace/.agents/skills").exists())
        self.assertTrue((engineering / "workspace/.agents/skills/test/SKILL.md").exists())
        self.assertIn("Mandatory security", (baseline / "workspace/AGENTS.md").read_text())
        self.assertIn("DDD rules", (baseline / "workspace/AGENTS.md").read_text())
        self.assertNotIn(".agents/skills", (baseline / "workspace/AGENTS.md").read_text())
        (baseline / "workspace/domain/src/main/java/Thing.java").write_text("changed")
        self.assertEqual("class Thing {}", (engineering / "workspace/domain/src/main/java/Thing.java").read_text())
        harness.verified_seed(self.seed)
        subprocess.run(["git", "rev-parse", "HEAD"], cwd=baseline / "workspace", check=True, capture_output=True)

    def test_hidden_tests_and_solutions_are_not_in_task_packet(self):
        root = self.prepare()
        names = harness.inventory(root / "workspace")
        self.assertFalse(any("BenchmarkDomainInvariantTest" in name or "reference_solutions" in name for name in names))
        self.assertTrue((root / "workspace/TASK.md").is_file())
        self.assertFalse((root / "workspace/HINTS.md").exists())
        self.assertEqual("not-run", harness.read_json(root / "result.json")["status"])

    def test_output_cannot_overwrite_an_existing_run(self):
        self.prepare()
        with self.assertRaises(ValueError):
            self.prepare()

    def test_tampered_seed_is_rejected(self):
        (self.seed / "project/pom.xml").write_text("tampered")
        with self.assertRaises(ValueError):
            self.prepare()

    def test_symlinks_cannot_escape_the_workspace(self):
        root = self.prepare()
        (root / "workspace/leak").symlink_to(self.seed / "seed.json")
        with self.assertRaises(ValueError):
            self.submit(root)
        self.assertFalse((root / "submissions/0001").exists())
        (root / "workspace/leak").unlink()
        self.assertEqual(1, self.submit(root)["submission"])

    def test_submission_is_a_frozen_copy(self):
        root = self.prepare()
        candidate = root / "workspace/domain/src/main/java/Thing.java"
        candidate.write_text("first")
        self.submit(root)
        candidate.write_text("second")
        self.assertEqual("first", (root / "submissions/0001/domain/src/main/java/Thing.java").read_text())
        self.assertEqual(1, harness.read_json(root / "result.json")["metrics"]["filesChanged"])

    def test_tampered_submission_or_rubric_is_rejected(self):
        root = self.prepare()
        self.submit(root)
        (root / "submissions/0001/pom.xml").write_text("changed after submit")
        with self.assertRaises(ValueError):
            harness.evaluate(argparse.Namespace(run=root))
        (root / "acceptance/common/BenchmarkArchitectureTest.java").write_text("fake")
        with self.assertRaises(ValueError):
            harness.load_run(root)

    def test_applied_migrations_and_build_settings_are_protected(self):
        before = {"pom.xml": "a", "infra/persistence/src/main/resources/db/migration/V1__base.sql": "a"}
        after = dict(before)
        after["pom.xml"] = "b"
        after["infra/persistence/src/main/resources/db/migration/V1__base.sql"] = "b"
        after["infra/persistence/src/main/resources/db/migration/V2__add.sql"] = "new"
        self.assertEqual(2, len(harness.protected_changes(before, after)))

    def test_restores_original_tests_without_removing_new_tests(self):
        root = self.prepare()
        base = root / "workspace/start/src/test/java/com/example/benchmark/BaselineTest.java"
        base.write_text("weakened")
        new = base.with_name("NewTest.java")
        new.write_text("new test")
        harness.restore_oracle(self.seed / "project", root / "workspace")
        self.assertEqual("trusted test", base.read_text())
        self.assertTrue(new.is_file())

    def test_compilation_or_exit_zero_alone_never_passes(self):
        root = self.prepare()
        state = harness.read_json(root / "run.json")
        result = harness.assess(root / "workspace", state["scenario"], 0)
        self.assertEqual("failed", result["status"])
        self.assertEqual("unavailable", result["architectureViolations"])

    def test_all_required_cases_must_run_and_pass(self):
        root = self.prepare()
        scenario = harness.read_json(root / "run.json")["scenario"]
        self.successful_reports(root / "workspace", scenario)
        self.assertEqual("passed", harness.assess(root / "workspace", scenario, 0)["status"])
        self.assertEqual("failed", harness.assess(root / "workspace", scenario, 1)["status"])
        path = root / "workspace/start/target/surefire-reports/TEST-fixture.xml"
        tree = ET.parse(path)
        ET.SubElement(tree.getroot()[0], "skipped")
        tree.write(path)
        self.assertEqual("failed", harness.assess(root / "workspace", scenario, 0)["status"])

    def test_missing_architecture_diagnostic_is_not_zero(self):
        root = self.prepare()
        scenario = harness.read_json(root / "run.json")["scenario"]
        self.successful_reports(root / "workspace", scenario)
        (root / "workspace/start/target/benchmark-architecture.json").unlink()
        result = harness.assess(root / "workspace", scenario, 0)
        self.assertFalse(result["taskSuccess"])
        self.assertEqual("unavailable", result["architectureViolations"])

    def test_execution_errors_are_not_counted_as_security_violations(self):
        root = self.prepare(scenario="add-api")
        scenario = harness.read_json(root / "run.json")["scenario"]
        self.successful_reports(root / "workspace", scenario)
        method = next(method["name"] for entry in scenario["acceptance"] for method in entry["methods"] if method["category"] == "security")
        path = root / "workspace/start/target/surefire-reports/TEST-fixture.xml"
        tree = ET.parse(path)
        for case in tree.getroot():
            if case.get("name") == method:
                ET.SubElement(case, "error", message="Database unavailable")
        tree.write(path)
        result = harness.assess(root / "workspace", scenario, 1)
        self.assertEqual("failed", result["status"])
        self.assertEqual("unavailable", result["securityViolations"])

    def test_environment_drift_blocks_evaluation(self):
        root = self.prepare()
        self.submit(root)
        with patch.object(harness.platform, "platform", return_value="different host"):
            result = harness.evaluate(argparse.Namespace(run=root))
        self.assertEqual("blocked", result["status"])
        self.assertEqual("different host", result["environment"]["os"])

    def test_docker_failure_is_blocked_and_does_not_run_maven(self):
        root = self.prepare(scenario="add-api")
        self.submit(root)
        with patch.object(harness, "java_version", return_value="fixture JDK"), patch.object(harness, "execute", return_value={"exitCode": 1}) as run:
            result = harness.evaluate(argparse.Namespace(run=root))
        self.assertEqual("blocked", result["status"])
        self.assertEqual(1, run.call_count)
        self.assertEqual("unavailable", harness.read_json(root / "result.json")["metrics"]["firstPassSuccess"])

    def test_repeated_evaluation_uses_frozen_submission_and_keeps_workspace_untouched(self):
        root = self.prepare()
        self.submit(root)
        (root / "workspace/domain/src/main/java/Thing.java").write_text("unsubmitted change")
        scenario = harness.read_json(root / "run.json")["scenario"]
        def build(command, project, log, env):
            self.assertEqual("class Thing {}", (project / "domain/src/main/java/Thing.java").read_text())
            self.assertEqual("trusted test", (project / "start/src/test/java/com/example/benchmark/BaselineTest.java").read_text())
            self.successful_reports(project, scenario)
            return {"exitCode": 0}
        with patch.object(harness, "execute", side_effect=build):
            for attempt in (1, 2):
                result = harness.evaluate(argparse.Namespace(run=root))
                self.assertEqual("passed", result["status"])
                self.assertEqual(attempt, result["attempt"])
        self.assertEqual("unsubmitted change", (root / "workspace/domain/src/main/java/Thing.java").read_text())

    def test_failed_first_submission_is_not_rewritten_by_later_success(self):
        root = self.prepare()
        _, state, _ = harness.load_run(root)
        state["evaluations"] = [{"submission": 1, "status": "failed"}, {"submission": 2, "status": "passed", "taskSuccess": True}]
        state["status"] = "passed"
        report = harness.summarize(root, state)
        self.assertFalse(report["metrics"]["firstPassSuccess"])
        self.assertTrue(report["metrics"]["taskSuccess"])

    def test_tokens_and_interventions_are_unavailable_until_recorded(self):
        root = self.prepare()
        report = harness.read_json(root / "result.json")
        self.assertEqual("unavailable", report["metrics"]["tokenUsage"])
        args = argparse.Namespace(run=root, human_interventions=2, input_tokens=10, output_tokens=None)
        report = harness.record(args)
        self.assertEqual("unavailable", report["metrics"]["tokenUsage"]["total"])
        args.input_tokens, args.output_tokens = None, 5
        self.assertEqual(15, harness.record(args)["metrics"]["tokenUsage"]["total"])
        self.assertEqual("unavailable", harness.record(args)["metrics"]["totalValidationAttempts"])
        args.local_validation_attempts = 4
        self.assertEqual(4, harness.record(args)["metrics"]["totalValidationAttempts"])

    def test_comparison_rejects_missing_or_different_controls(self):
        first = self.prepare("baseline", mode="baseline")
        second = self.prepare("engineering")
        result = harness.compare(argparse.Namespace(runs=[first, second]))
        self.assertEqual("blocked", result["status"])
        self.assertIn("model", result["unavailableControls"])
        report = harness.read_json(second / "result.json")
        report["taskFingerprint"] = "changed-task"
        harness.write_json(second / "result.json", report)
        self.assertIn("taskFingerprint", harness.compare(argparse.Namespace(runs=[first, second]))["differentControls"])

    def test_scenario_manifest_paths_and_methods_are_valid(self):
        for scenario in harness.SCENARIOS:
            value = harness.read_json(harness.BENCHMARKS / "scenarios" / scenario / "scenario.json")
            self.assertEqual(scenario, value["id"])
            names = set()
            for entry in value["acceptance"]:
                source = harness.checked_child(HARNESS / "acceptance", entry["source"])
                text = source.read_text()
                for method in entry["methods"]:
                    self.assertIn("void " + method["name"] + "(", text)
                    self.assertIn(method["category"], {"task", "security", "architecture"})
                    names.add(method["name"])
            self.assertTrue(set(value["mutantRejectedBy"]).issubset(names))


if __name__ == "__main__":
    unittest.main()
