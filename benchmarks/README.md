# Offline agent benchmark

This maintainer-only harness measures delivery on a fixed generated application. It calls no model API, adds no runtime dependency, and never changes the archetype's production templates. Python 3.9+, Git, the POM's JDK/Maven, and Docker for API/persistence acceptance are required. Maven Wrapper downloads remain checksum-pinned.

## Tasks and oracle

| Scenario | Public task | Independent acceptance |
| --- | --- | --- |
| `add-api` | [Update a user's email](scenarios/add-api/task.md) | HTTP validation, caller and use-case permissions, tenant isolation, stored result and uniqueness |
| `domain-invariant` | [Review a locked user before activation](scenarios/domain-invariant/task.md) | Aggregate transition, rejected state/events, reconstruction and existing legal transitions |
| `persistence-change` | [Persist a preferred language](scenarios/persistence-change/task.md) | Existing-row Flyway upgrade, defaults, mappings, query output, tenant isolation and optimistic locking |

Each directory separates `task.md`, optional neutral `hints.md`, and a machine-readable rubric. Acceptance Java and evaluator calibration solutions live in `harness/`, outside the task packet. The harness freezes a rubric copy before the task and injects its tests only into a separate evaluation copy after submission. It restores original tests and rejects changes to POMs, wrappers, verification scripts, and already-applied migrations. For the API task only, the evaluator adds an unused method to the original gRPC test double so the expanded Facade interface compiles; this adapter preserves all original assertions and supplies no email behavior. These three tasks need no build-infrastructure changes.

Only give the measured agent access to its `workspace/`. Keep the parent run directory, seed, evaluator sources and reference solutions outside its allowed filesystem context. This separation is an evaluation protocol, not an operating-system security boundary; use your runner's directory permissions or container controls for untrusted agents. An unrestricted agent on the same host could read the oracle. Never give it this repository's entire benchmark tree as task context.

## Create a reproducible baseline

From the archetype repository, choose one exact commit for every comparable run. A seed uses `git archive` of that commit, not the current dirty worktree, then uses that snapshot's wrapper to install and generate `com.example.benchmark:benchmark-app`. The official archetype fixture still compiles/tests during installation.

```bash
# Select the JDK required by pom.xml before these commands.
python3 benchmarks/harness/run.py seed \
  --revision HEAD --output /tmp/atom-benchmark-seed \
  --cache-from "$HOME/.m2/repository"
```

The optional cache source is copied, never linked or changed. The seed and each evaluated run have their own writable Maven repository and wrapper cache under their disposable output root. These copies use disk space; omit `--cache-from` to download into an empty seed cache. Retain outputs while comparing runs and remove only the output directories you created when finished.

Seed files are hashed and rechecked before every preparation/submission/evaluation. New runs copy clean sources and start independent Git histories, with no targets or previous agent changes. Existing output directories are rejected. Use the same seed for both modes and all repetitions.

## Run a task manually with any coding agent

```bash
python3 benchmarks/harness/run.py prepare \
  --seed /tmp/atom-benchmark-seed --scenario add-api --mode engineering \
  --output /tmp/email-engineering --model YOUR_MODEL --model-version EXACT_VERSION \
  --environment-id YOUR_AGENT_RUNNER_SETTINGS_AND_RESOURCE_PROFILE
```

Point your agent at `/tmp/email-engineering/workspace`, and give it `TASK.md`. Let it edit that workspace and run its usual tests. Do not show it the hidden acceptance sources or reference implementation. Use `--hints` during preparation only if the same hints are supplied in both comparison groups. Preparation itself never starts an agent or a paid API call.

When the agent reports a complete solution, freeze the first submission and evaluate it:

```bash
python3 benchmarks/harness/run.py submit --run /tmp/email-engineering
python3 benchmarks/harness/run.py evaluate --run /tmp/email-engineering
```

Each submission and evaluation gets a new directory; rerunning evaluation cannot change the submitted snapshot or original workspace. Evaluation runs `clean test`, checks actual Surefire/Failsafe XML and every required acceptance method, and requires both the original architecture gate and the independent architecture diagnostic. Maven exit zero or compilation alone is insufficient. Missing/skipped required checks fail. A missing JDK, mismatched JDK or unavailable Docker blocks evaluation without declaring a task failure. Other build failures remain failed with a diagnostic log; dependency-resolution problems may need human diagnosis.

After a failed complete submission, let the agent repair its workspace, then call `submit` and `evaluate` again. First-pass success remains tied to submission 1's first conclusive evaluation; blocked attempts do not decide it. A new unevaluated submission has status `not-run`, even if an earlier submission passed.

```bash
# Optional observed values; omit anything the runner cannot measure.
python3 benchmarks/harness/run.py record --run /tmp/email-engineering \
  --human-interventions 1 --input-tokens 12000 --output-tokens 1800
python3 benchmarks/harness/run.py summary --run /tmp/email-engineering
```

The numeric values above illustrate command syntax, not measured results. Commands print one JSON document to stdout; evaluation logs are retained under the run's `evaluations/`. Exit codes are 0 for successful commands or `not-run` preparation/submission, 1 for failure, and 2 for known environment blocking. Every run has `result.json` and `summary.md`.

## Controlled comparison and metric meaning

Prepare a second fresh workspace with `--mode baseline` and identical scenario, seed, model version, hints and execution environment. Baseline removes `.agents/skills/` and its discovery links; both modes retain the architecture/security instructions in `AGENTS.md`, the general docs and existing build commands. This measures **Skills' incremental contribution**, not the whole AI engineering foundation. A broader comparison would require an additional control without the agent instructions; this harness makes no claim about that experiment.

```bash
python3 benchmarks/harness/run.py compare /tmp/email-baseline /tmp/email-engineering
```

Comparison checks the actual task/hints fingerprint as well as the rubric and seed. It refuses attribution when recorded controls differ, model/version or the operator's agent-environment profile is unavailable, either run is incomplete/blocked, or both modes are not represented. The harness records its actual OS/Python/JDK at preparation and checks for drift at evaluation. Keep machine/container resources, network, Docker images, agent permissions, model settings and time limits equal as well; the recorded OS/JDK cannot prove those external conditions. Use repeated paired trials, not a single pair, before attributing a benefit.

| Metric | Meaning |
| --- | --- |
| Task success | All independent required behaviors and architecture gates pass, with successful Maven execution |
| First-pass success | First conclusive evaluation of the first complete submission |
| Architecture violations | Deduplicated findings from the trusted baseline's semantic bytecode checks; unavailable if diagnostic execution fails |
| Security violations | Assertion failures in independent permission/tenant checks; unavailable on execution errors, missing/skipped checks, or scenarios with none |
| Test coverage | Passed/total independent required behavior checks, **not** line coverage or proof of candidate-authored test quality |
| Human interventions | Operator-recorded corrections; unavailable until supplied |
| Duration | Wall time from preparation to latest submission, including pauses; evaluation duration is recorded separately |
| Tokens | Observed input/output; total only when both are known; unavailable never means zero |
| Files changed | Source-tree differences from that mode's prepared workspace, excluding build output and Git internals |
| Validation attempts | `validationAttempts` counts evaluator invocations, including blocked ones. Agent-local and total attempts remain unavailable unless the operator supplies `--local-validation-attempts` |

The evaluator deliberately does not grade style, abstraction quality, all possible security defects, or line coverage. Review those separately. Candidate-added tests run alongside restored original tests, but changes to original test files are restored for oracle integrity; the independent coverage metric must not be described as candidate test coverage.

## Test the harness without a model

Normal CI runs only deterministic, quick harness tests and existing application gates:

```bash
python3 -m unittest discover -s benchmarks/harness/tests -v
```

After changing tasks, the oracle, or the generated production contracts, calibrate the Java acceptance tests against correct and deliberately defective solutions:

```bash
python3 benchmarks/harness/run.py self-test \
  --seed /tmp/atom-benchmark-seed --output /tmp/atom-benchmark-calibration
```

Calibration creates six independent runs, three per variant, and requires every correct solution to pass and every mutant to fail its declared behavioral checks. The API mutant bypasses write authority, the domain mutant permits direct unlocking, and the persistence mutant loses the language during reconstruction. It requires Docker and a full build environment but no model. Its `calibration.json` is evidence about the evaluator, **not** an agent benchmark score. Probabilistic model evaluations are never ordinary PR gates.
