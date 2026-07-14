package ${package};

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bytecode-level guards for dependency, adapter, entity, and naming boundaries. */
class ArchitectureBoundaryTest {

    private static final String ROOT_PACKAGE = "${package}";

    @Test
    void compiledArchitectureRespectsBoundaries() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT_PACKAGE);
        List<String> violations = new ArrayList<>();

        assertLayerPresent(classes, ".api.", violations);
        assertLayerPresent(classes, ".application.", violations);
        assertLayerPresent(classes, ".domain.", violations);
        assertLayerPresent(classes, ".shared.", violations);
        assertLayerPresent(classes, ".infra.persistence.", violations);
        assertLayerPresent(classes, ".infra.rest.", violations);

        for (JavaClass source : classes) {
            inspectDependencies(source, violations);
            inspectEntityMethods(source, violations);
            inspectNaming(source, violations);
        }

        assertTrue(violations.isEmpty(), () -> "Architecture boundary violations:\n"
                + String.join("\n", violations));
    }

    private static void inspectDependencies(JavaClass source, List<String> violations) {
        String sourcePackage = source.getPackageName();
        List<String> forbidden = forbiddenDependencies(sourcePackage);
        source.getDirectDependenciesFromSelf().forEach(dependency -> {
            String target = dependency.getTargetClass().getName();
            if (forbidden.stream().anyMatch(target::startsWith)) {
                violations.add(source.getName() + " -> " + target);
            }
            if (sourcePackage.startsWith(ROOT_PACKAGE + ".infra.rest.controller")
                    && (target.startsWith(ROOT_PACKAGE + ".domain.repository.")
                    || target.startsWith(ROOT_PACKAGE + ".infra.persistence.repository.")
                    || target.startsWith(ROOT_PACKAGE + ".application.vo."))) {
                violations.add("REST controller bypasses facade/use-case boundary: "
                        + source.getName() + " -> " + target);
            }
        });
    }

    private static List<String> forbiddenDependencies(String sourcePackage) {
        if (sourcePackage.startsWith(ROOT_PACKAGE + ".domain")) {
            return List.of(
                    ROOT_PACKAGE + ".api.", ROOT_PACKAGE + ".application.",
                    ROOT_PACKAGE + ".shared.", ROOT_PACKAGE + ".infra.",
                    "org.springframework.", "org.mybatis.", "com.baomidou.");
        }
        if (sourcePackage.startsWith(ROOT_PACKAGE + ".api")) {
            return List.of(
                    ROOT_PACKAGE + ".application.", ROOT_PACKAGE + ".domain.",
                    ROOT_PACKAGE + ".infra.", "org.springframework.",
                    "org.mybatis.", "com.baomidou.");
        }
        if (sourcePackage.startsWith(ROOT_PACKAGE + ".shared")) {
            return List.of(
                    ROOT_PACKAGE + ".api.", ROOT_PACKAGE + ".application.",
                    ROOT_PACKAGE + ".domain.", ROOT_PACKAGE + ".infra.",
                    "org.springframework.", "org.mybatis.", "com.baomidou.");
        }
        if (sourcePackage.startsWith(ROOT_PACKAGE + ".application")) {
            return List.of(
                    ROOT_PACKAGE + ".infra.", "org.springframework.data.",
                    "org.springframework.web.", "org.mybatis.",
                    "com.baomidou.", "reactor.netty.");
        }
        return List.of();
    }

    private static void inspectEntityMethods(JavaClass source, List<String> violations) {
        if (!source.getPackageName().startsWith(ROOT_PACKAGE + ".domain.entity")) {
            return;
        }
        source.getMethods().stream()
                .filter(method -> method.getOwner().equals(source))
                .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                .filter(method -> method.getName().matches("set[A-Z].*"))
                .forEach(method -> violations.add("Domain entity exposes public setter: " + method.getFullName()));
    }

    private static void inspectNaming(JavaClass source, List<String> violations) {
        if (source.getSimpleName().equals("package-info")) {
            return;
        }
        requireSuffix(source, ".api.dto.request", "Request", violations);
        requireSuffix(source, ".api.dto.response", "Response", violations);
        requireSuffix(source, ".application.vo", "VO", violations);
        requireSuffix(source, ".infra.persistence.mysql.po", "PO", violations);
        if (source.isInterface()) {
            requireSuffix(source, ".domain.repository", "Repository", violations);
        }
        requireSuffix(source, ".infra.persistence.repository", "RepositoryImpl", violations);
    }

    private static void requireSuffix(
            JavaClass source, String packageSuffix, String nameSuffix, List<String> violations) {
        if (source.getPackageName().startsWith(ROOT_PACKAGE + packageSuffix)
                && !source.getSimpleName().endsWith(nameSuffix)) {
            violations.add(source.getName() + " must end with " + nameSuffix);
        }
    }

    private static void assertLayerPresent(
            JavaClasses classes, String packageFragment, List<String> violations) {
        boolean present = classes.stream()
                .anyMatch(javaClass -> javaClass.getPackageName().startsWith(ROOT_PACKAGE + packageFragment));
        if (!present) {
            violations.add("No compiled classes found for layer " + packageFragment);
        }
    }
}
