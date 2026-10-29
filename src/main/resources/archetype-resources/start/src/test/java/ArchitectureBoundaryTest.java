package ${package};

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;
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
                .withImportOption(ArchitectureBoundaryTest::isProductionLocation)
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
            inspectPersistenceObjects(source, violations);
            inspectNaming(source, violations);
        }

        assertTrue(violations.isEmpty(), () -> "Architecture boundary violations:\n"
                + String.join("\n", violations));
    }

    static boolean isProductionLocation(Location location) {
        // The archetype IT nests the whole reactor below an outer target/test-classes.
        // Only the innermost Maven output directory identifies production vs test code.
        String uri = location.asURI().toString();
        return location.isJar() || uri.lastIndexOf("/target/test-classes/") < 0
                || uri.lastIndexOf("/target/classes/") > uri.lastIndexOf("/target/test-classes/");
    }

    static void inspectDependencies(JavaClass source, List<String> violations) {
        String sourcePackage = source.getPackageName();
        List<String> forbidden = forbiddenDependencies(sourcePackage);
        source.getDirectDependenciesFromSelf().forEach(dependency -> {
            String target = dependency.getTargetClass().getName();
            if (forbidden.stream().anyMatch(target::startsWith)) {
                violations.add(source.getName() + " -> " + target);
            }
            if ((inPackage(sourcePackage, ROOT_PACKAGE + ".infra.rest.controller")
                    || source.isAnnotatedWith("org.springframework.stereotype.Controller")
                    || source.isMetaAnnotatedWith("org.springframework.stereotype.Controller"))
                    && (target.startsWith(ROOT_PACKAGE + ".domain.repository.")
                    || target.startsWith(ROOT_PACKAGE + ".domain.entity.")
                    || target.startsWith(ROOT_PACKAGE + ".domain.aggregate.")
                    || target.startsWith(ROOT_PACKAGE + ".infra.persistence.")
                    || target.startsWith(ROOT_PACKAGE + ".application.vo."))) {
                violations.add("REST controller bypasses facade/use-case boundary: "
                        + source.getName() + " -> " + target);
            }
            if (inPackage(sourcePackage, ROOT_PACKAGE + ".infra.grpc")
                    && (target.startsWith(ROOT_PACKAGE + ".domain.")
                    || target.startsWith(ROOT_PACKAGE + ".application.")
                    || target.startsWith(ROOT_PACKAGE + ".infra.persistence.")
                    || target.startsWith(ROOT_PACKAGE + ".infra.rest."))) {
                violations.add("gRPC adapter bypasses the facade boundary: "
                        + source.getName() + " -> " + target);
            }
        });
    }

    private static List<String> forbiddenDependencies(String sourcePackage) {
        if (inPackage(sourcePackage, ROOT_PACKAGE + ".domain")) {
            return List.of(
                    ROOT_PACKAGE + ".api.", ROOT_PACKAGE + ".application.",
                    ROOT_PACKAGE + ".shared.", ROOT_PACKAGE + ".infra.",
                    "org.springframework.", "org.mybatis.", "com.baomidou.",
                    "java.net.http.", "java.sql.", "javax.sql.",
                    "jakarta.persistence.", "javax.persistence.",
                    "jakarta.servlet.", "javax.servlet.",
                    "org.apache.http.", "org.apache.hc.", "okhttp3.",
                    "io.grpc.", "io.netty.", "reactor.netty.",
                    "redis.clients.", "io.lettuce.", "org.redisson.");
        }
        if (inPackage(sourcePackage, ROOT_PACKAGE + ".api")) {
            return List.of(
                    ROOT_PACKAGE + ".application.", ROOT_PACKAGE + ".domain.",
                    ROOT_PACKAGE + ".infra.", "org.springframework.",
                    "org.mybatis.", "com.baomidou.");
        }
        if (inPackage(sourcePackage, ROOT_PACKAGE + ".shared")) {
            return List.of(
                    ROOT_PACKAGE + ".api.", ROOT_PACKAGE + ".application.",
                    ROOT_PACKAGE + ".domain.", ROOT_PACKAGE + ".infra.",
                    "org.springframework.", "org.mybatis.", "com.baomidou.");
        }
        if (inPackage(sourcePackage, ROOT_PACKAGE + ".application")) {
            return List.of(
                    ROOT_PACKAGE + ".infra.", "org.springframework.data.",
                    "org.springframework.web.", "org.mybatis.",
                    "com.baomidou.", "reactor.netty.");
        }
        return List.of();
    }

    static void inspectEntityMethods(JavaClass source, List<String> violations) {
        if (!inPackage(source.getPackageName(), ROOT_PACKAGE + ".domain.entity")
                && !source.isAssignableTo(ROOT_PACKAGE + ".domain.aggregate.AggregateRoot")) {
            return;
        }
        source.getAllMethods().stream()
                .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                .filter(method -> method.getName().matches("set[A-Z].*"))
                .forEach(method -> violations.add("Domain entity exposes public setter: " + method.getFullName()));
    }

    static void inspectPersistenceObjects(JavaClass source, List<String> violations) {
        if (!inPackage(source.getPackageName(), ROOT_PACKAGE + ".infra.persistence.mysql.po")
                || source.getSimpleName().equals("package-info")) {
            return;
        }
        if (!source.isAssignableTo(ROOT_PACKAGE + ".infra.persistence.mysql.po.BasePO")) {
            violations.add("Persistence object must extend BasePO: " + source.getName());
        }
        source.getDirectDependenciesFromSelf().stream()
                .filter(dependency -> dependency.getTargetClass().getName().startsWith(ROOT_PACKAGE + ".domain."))
                .forEach(dependency -> violations.add("Persistence object leaks domain type: "
                        + dependency.getDescription()));
    }

    private static boolean inPackage(String actual, String expected) {
        return actual.equals(expected) || actual.startsWith(expected + ".");
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
        if (inPackage(source.getPackageName(), ROOT_PACKAGE + packageSuffix)
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
