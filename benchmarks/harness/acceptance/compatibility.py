"""Narrow evaluator-owned adaptations for legitimate scenario interface extensions."""
from pathlib import Path

STUB = "infra/grpc/src/test/java/com/example/benchmark/infra/grpc/user/UserGrpcServiceTest.java"
ANCHOR = "        @Override\n        public Result<Void> deleteUser(Long userId) {"
ADDITION = '''        // Compatibility with the benchmark's HTTP-only facade extension.
        // No @Override: the unimplemented baseline must still compile for behavioral evaluation.
        public Result<Void> updateUserEmail(Long userId, String email) {
            throw new UnsupportedOperationException("Email updates are not part of the gRPC test stub");
        }

'''


def apply(project, scenario):
    """After oracle restoration, extend only the existing add-api test double.

    All existing test assertions and recording behavior remain byte-for-byte intact.
    The extra method is intentionally unusable, so it cannot satisfy email acceptance.
    """
    if scenario != "add-api":
        return False
    path = Path(project) / STUB
    original = path.read_text(encoding="utf-8")
    if ADDITION in original:
        return False
    if original.count(ANCHOR) != 1 or "updateUserEmail(" in original:
        raise ValueError("Trusted gRPC test stub no longer matches the compatibility adapter")
    path.write_text(original.replace(ANCHOR, ADDITION + ANCHOR), encoding="utf-8")
    return True
