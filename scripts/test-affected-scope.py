#!/usr/bin/env python3
"""Deterministic selection/process tests using real independent Git repositories."""

import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import sys
import tempfile
import time
import unittest

REPOSITORY = Path(__file__).resolve().parents[1]
SCRIPTS = REPOSITORY / "src/main/resources/archetype-resources/scripts"
SELECTOR = SCRIPTS / "affected-scope.py"
RUNNER = SCRIPTS / "agent-check.sh"
GIT = shutil.which("git")
BASH = shutil.which("bash")
DOMAIN_TEST = "domain/src/test/java/example/domain/EmailTest.java"
APPLICATION_TEST = "application/src/test/java/example/application/assembler/MessageTest.java"


class AffectedScopeTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="atom-affected-test-")
        self.addCleanup(self.temporary.cleanup)
        self.base = Path(self.temporary.name)
        self.project = self.base / "project"
        self.project.mkdir()
        self.env = dict(os.environ, GIT_CONFIG_NOSYSTEM="1", GIT_CONFIG_GLOBAL=os.devnull,
                        GIT_AUTHOR_NAME="Scope Test", GIT_AUTHOR_EMAIL="scope@example.invalid",
                        GIT_COMMITTER_NAME="Scope Test", GIT_COMMITTER_EMAIL="scope@example.invalid")
        self.git("init", "-q")
        for path, content in {
            "pom.xml": "<project/>\n", "domain/pom.xml": "<project/>\n",
            "application/pom.xml": "<project/>\n", ".gitignore": "target/\n",
            "docs/guide.md": "Guide\n", "AGENTS.md": "Rules\n",
            DOMAIN_TEST: "class EmailTest {}\n", APPLICATION_TEST: "class MessageTest {}\n",
            "mvnw": "exit 0\n",
        }.items():
            self.write(path, content)
        (self.project / "start").mkdir()
        self.commit()

    def git(self, *args):
        return subprocess.check_output([GIT, "-c", "core.hooksPath=" + os.devnull, *args],
                                       cwd=self.project, env=self.env, stderr=subprocess.DEVNULL)

    def write(self, path, text):
        file = self.project / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(text, encoding="utf-8")

    def commit(self):
        self.git("add", "--all")
        self.git("commit", "-q", "-m", "fixture")

    def selection(self, context="application", directory=None):
        output = subprocess.check_output([sys.executable, str(SELECTOR), context],
                                         cwd=directory or self.project, env=self.env, text=True)
        return tuple(output.strip().split())

    def assert_full(self):
        self.assertEqual(self.selection()[0], "full")

    def test_tracked_domain_test(self):
        self.write(DOMAIN_TEST, "class EmailTest { int updated; }\n")
        self.assertEqual(self.selection(), ("module-tests", "domain", "single-module-tests-only"))

    def test_staged_application_test(self):
        self.write(APPLICATION_TEST, "class MessageTest { int updated; }\n")
        self.git("add", APPLICATION_TEST)
        self.assertEqual(self.selection()[0:2], ("module-tests", "application"))

    def test_staged_edit_reversed_in_worktree_is_not_lost(self):
        self.write(DOMAIN_TEST, "class EmailTest { int updated; }\n")
        self.git("add", DOMAIN_TEST)
        self.write(DOMAIN_TEST, "class EmailTest {}\n")
        self.assertEqual(self.selection()[0:2], ("module-tests", "domain"))

    def test_untracked_test_and_prose(self):
        self.write("domain/src/test/java/example/domain/NewTest.java", "class NewTest {}\n")
        self.write("docs/new.md", "New guide\n")
        self.assertEqual(self.selection()[0:2], ("module-tests", "domain"))

    def test_deleted_test(self):
        (self.project / DOMAIN_TEST).unlink()
        self.assertEqual(self.selection()[0:2], ("module-tests", "domain"))

    def test_renamed_test(self):
        self.git("mv", DOMAIN_TEST, "domain/src/test/java/example/domain/RenamedTest.java")
        self.assertEqual(self.selection()[0:2], ("module-tests", "domain"))

    def test_renamed_between_modules(self):
        self.git("mv", DOMAIN_TEST, "application/src/test/java/example/application/assembler/MovedTest.java")
        self.assert_full()

    def test_cross_module_changes(self):
        self.write(DOMAIN_TEST, "class EmailTest { int updated; }\n")
        self.write(APPLICATION_TEST, "class MessageTest { int updated; }\n")
        self.assert_full()

    def test_parent_and_module_poms(self):
        for path in ("pom.xml", "domain/pom.xml", "application/pom.xml"):
            with self.subTest(path=path):
                self.git("reset", "--hard", "HEAD")
                self.write(path, "<project><version>2</version></project>\n")
                self.assert_full()

    def test_sensitive_and_unknown_paths(self):
        paths = ["api/src/main/java/Contract.java", "shared/src/test/java/UtilityTest.java",
                 "domain/src/main/java/example/domain/valueobject/Email.java",
                 "application/src/main/java/example/application/service/MessageService.java",
                 "application/src/test/java/example/application/security/PolicyTest.java",
                 "infra/persistence/src/main/resources/db/migration/V2__change.sql",
                 "start/src/test/java/ArchitectureBoundaryTest.java", "unknown.dat"]
        for path in paths:
            with self.subTest(path=path):
                self.write(path, "change\n")
                self.assert_full()
                (self.project / path).unlink()

    def test_sensitive_content_in_each_git_state(self):
        self.write(DOMAIN_TEST, "class EmailTest { TenantId tenant; }\n")
        self.assert_full()
        self.git("add", DOMAIN_TEST)
        self.write(DOMAIN_TEST, "class EmailTest {}\n")
        self.assert_full()  # Index contents still require full validation.
        self.write(DOMAIN_TEST, "class EmailTest { TenantId tenant; }\n")
        self.commit()
        (self.project / DOMAIN_TEST).unlink()
        self.assert_full()  # Deleting a sensitive test must not hide its old contents.

    def test_no_changes(self):
        self.assert_full()

    def test_no_history_and_no_git(self):
        shutil.rmtree(self.project / ".git")
        self.assert_full()
        self.git("init", "-q")
        self.assert_full()

    def test_nested_project_does_not_use_parent_git(self):
        nested = self.project / "target/generated-app"
        nested.mkdir(parents=True)
        self.assertEqual(self.selection(directory=nested)[0], "full")

    def test_unmerged_files(self):
        self.git("checkout", "-q", "-b", "other")
        self.write(DOMAIN_TEST, "class Other {}\n")
        self.commit()
        self.git("checkout", "-q", "-")
        self.write(DOMAIN_TEST, "class Current {}\n")
        self.commit()
        with self.assertRaises(subprocess.CalledProcessError):
            self.git("merge", "other")
        self.assert_full()

    def test_symlink_and_control_character_path(self):
        link = self.project / "domain/src/test/java/example/domain/Linked.java"
        link.symlink_to(self.project / DOMAIN_TEST)
        self.assert_full()
        link.unlink()
        self.write("domain/src/test/java/example/domain/Bad\nName.java", "class Other {}\n")
        self.assert_full()

    def test_archetype_prose_and_template_changes(self):
        self.write("docs/guide.md", "Updated guide\n")
        self.assertEqual(self.selection("archetype")[0], "docs")
        self.write("src/main/resources/archetype-resources/.agents/skills/example/SKILL.md", "Guide\n")
        self.assertEqual(self.selection("archetype")[0], "generate")
        self.write("src/main/resources/archetype-resources/Makefile", "target:\n")
        self.assertEqual(self.selection("archetype")[0], "full")

    def prepare_runner(self):
        self.write("mvnw", '''#!/bin/sh
printf '%s|%s\\n' "$CI" "$*" >> "$TRACE"
mkdir -p domain/target/surefire-reports
printf '<testsuite name="%s" tests="1"/>\\n' "$CI" > domain/target/surefire-reports/TEST-Fixture.xml
if [ "${WAIT_FOR_SIGNAL:-no}" = yes ]; then
    touch "$READY"
    sleep 30
fi
exit 0
''')
        self.commit()
        bin_dir = self.base / "bin"
        bin_dir.mkdir()
        docker = bin_dir / "docker"
        docker.write_text('#!/bin/sh\nprintf "docker\\n" >> "$TRACE"\nexit "${DOCKER_EXIT:-0}"\n')
        docker.chmod(0o755)
        tmpdir = self.base / 'reports "quoted" \\ 名字'
        tmpdir.mkdir()
        self.env.update(PATH=str(bin_dir) + os.pathsep + self.env["PATH"], TMPDIR=str(tmpdir),
                        TRACE=str(self.base / "trace"), READY=str(self.base / "ready"))
        return bin_dir

    def run_affected(self):
        process = subprocess.run([BASH, str(RUNNER), "affected"], cwd=self.project, env=self.env,
                                 text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=20)
        result = json.loads(process.stdout)
        self.assertEqual(result["exitCode"], process.returncode, process.stderr)
        for path in result["reportPaths"]:
            self.assertTrue(Path(path).is_dir(), path)
            self.assertTrue(list(Path(path).glob("TEST-*.xml")), path)
        return process, result

    def test_real_git_selection_drives_reactor(self):
        self.prepare_runner()
        self.write(DOMAIN_TEST, "class EmailTest { int updated; }\n")
        process, result = self.run_affected()
        self.assertEqual(process.returncode, 0, process.stderr)
        self.assertEqual(result["scope"], "affected")
        self.assertEqual(result["selectedScope"], "module-tests")
        self.assertEqual(result["selectedModules"], ["domain"])
        trace = (self.base / "trace").read_text()
        self.assertIn("false|-B -ntp -pl domain -am test", trace)
        self.assertNotIn("docker", trace)

    def test_full_fallback_preserves_unit_and_integration_reports(self):
        self.prepare_runner()
        self.write("unknown.data", "unknown\n")
        process, result = self.run_affected()
        self.assertEqual(process.returncode, 0, process.stderr)
        self.assertEqual(result["selectedScope"], "full")
        checks = {check["name"]: check for check in result["checks"]}
        for name, marker in (("unit-and-architecture-tests", "false"), ("integration-tests", "true")):
            report = Path(checks[name]["reportPaths"][0]) / "TEST-Fixture.xml"
            self.assertIn(f'name="{marker}"', report.read_text())

    def test_full_fallback_is_blocked_without_docker(self):
        self.prepare_runner()
        self.env["DOCKER_EXIT"] = "1"
        self.write("unknown.data", "unknown\n")
        process, result = self.run_affected()
        self.assertEqual(process.returncode, 2)
        self.assertEqual(result["status"], "blocked")
        self.assertNotIn("true|", (self.base / "trace").read_text())

    def test_no_python_falls_back_to_full(self):
        bin_dir = self.prepare_runner()
        for command in ("git", "sh", "dirname", "find", "mktemp", "mkdir", "cp", "rm"):
            (bin_dir / command).symlink_to(shutil.which(command))
        self.env["PATH"] = str(bin_dir)
        self.write(DOMAIN_TEST, "class EmailTest { int updated; }\n")
        process, result = self.run_affected()
        self.assertEqual(process.returncode, 0, process.stderr)
        self.assertEqual(result["selectedScope"], "full")
        self.assertEqual(result["selectionReason"], "selector-unavailable")

    def test_sigint_returns_failed_json(self):
        self.prepare_runner()
        self.env["WAIT_FOR_SIGNAL"] = "yes"
        self.write(DOMAIN_TEST, "class EmailTest { int updated; }\n")
        process = subprocess.Popen([BASH, str(RUNNER), "affected"], cwd=self.project, env=self.env,
                                   text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   start_new_session=True)
        self.addCleanup(lambda: process.poll() is not None or os.killpg(process.pid, signal.SIGKILL))
        deadline = time.monotonic() + 5
        while not (self.base / "ready").exists() and process.poll() is None and time.monotonic() < deadline:
            time.sleep(0.01)
        self.assertTrue((self.base / "ready").exists())
        os.killpg(process.pid, signal.SIGINT)
        stdout, stderr = process.communicate(timeout=5)
        self.assertTrue(stdout, f"exit={process.returncode}; stderr={stderr}")
        result = json.loads(stdout)
        self.assertEqual(process.returncode, 130, stderr)
        self.assertEqual(result["exitCode"], 130)
        self.assertEqual(result["status"], "failed")
        self.assertTrue(any(check["status"] == "failed" for check in result["checks"]))


if __name__ == "__main__":
    unittest.main()
