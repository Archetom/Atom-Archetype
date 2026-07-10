package ${package};

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the source-level dependency rules documented by the generated project. */
class ArchitectureBoundaryTest {

    private static final String ROOT_PACKAGE = "${package}";

    @Test
    void modulesDoNotImportOuterLayers() throws IOException {
        String configuredRoot = System.getProperty("maven.multiModuleProjectDirectory");
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path fallbackRoot = Files.isDirectory(workingDirectory.resolve("domain"))
                ? workingDirectory : workingDirectory.getParent();
        Path reactorRoot = (configuredRoot == null ? fallbackRoot : Path.of(configuredRoot))
                .toAbsolutePath().normalize();

        List<BoundaryRule> rules = List.of(
                new BoundaryRule("domain", List.of(
                        ROOT_PACKAGE + ".api.",
                        ROOT_PACKAGE + ".application.",
                        ROOT_PACKAGE + ".shared.",
                        ROOT_PACKAGE + ".infra.",
                        "org.springframework.",
                        "org.mybatis.",
                        "com.baomidou.")),
                new BoundaryRule("api", List.of(
                        ROOT_PACKAGE + ".application.",
                        ROOT_PACKAGE + ".domain.",
                        ROOT_PACKAGE + ".infra.",
                        "org.springframework.",
                        "org.mybatis.",
                        "com.baomidou.")),
                new BoundaryRule("shared", List.of(
                        ROOT_PACKAGE + ".api.",
                        ROOT_PACKAGE + ".application.",
                        ROOT_PACKAGE + ".domain.",
                        ROOT_PACKAGE + ".infra.",
                        "org.springframework.",
                        "org.mybatis.",
                        "com.baomidou.")),
                new BoundaryRule("application", List.of(
                        ROOT_PACKAGE + ".infra.",
                        "org.springframework.data.",
                        "org.springframework.web.",
                        "org.mybatis.",
                        "com.baomidou.",
                        "reactor.netty."))
        );

        List<String> violations = new ArrayList<>();
        for (BoundaryRule rule : rules) {
            Path sourceRoot = reactorRoot.resolve(rule.module()).resolve("src/main/java");
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (var paths = Files.walk(sourceRoot)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> inspect(path, rule, violations));
            }
        }

        assertTrue(violations.isEmpty(), () -> "Architecture boundary violations:\n"
                + String.join("\n", violations));
    }

    private static void inspect(Path source, BoundaryRule rule, List<String> violations) {
        try {
            for (String line : Files.readAllLines(source)) {
                String candidate = line.trim();
                if (!candidate.startsWith("import ")) {
                    continue;
                }
                for (String forbiddenPrefix : rule.forbiddenImports()) {
                    if (candidate.startsWith("import " + forbiddenPrefix)) {
                        violations.add(rule.module() + ": " + source + " -> " + candidate);
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect " + source, exception);
        }
    }

    private record BoundaryRule(String module, List<String> forbiddenImports) {
    }
}
