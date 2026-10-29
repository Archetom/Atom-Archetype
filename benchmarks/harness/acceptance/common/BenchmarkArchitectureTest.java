package com.example.benchmark;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Counts the same trusted baseline rules that gate the generated application. */
class BenchmarkArchitectureTest {
    @Test
    void recordsActualArchitectureViolations() throws Exception {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ArchitectureBoundaryTest::isProductionLocation)
                .importPackages("com.example.benchmark");
        List<String> violations = new ArrayList<>();
        Method present = ArchitectureBoundaryTest.class.getDeclaredMethod(
                "assertLayerPresent", JavaClasses.class, String.class, List.class);
        Method naming = ArchitectureBoundaryTest.class.getDeclaredMethod(
                "inspectNaming", JavaClass.class, List.class);
        present.setAccessible(true);
        naming.setAccessible(true);
        for (String layer : List.of(".api.", ".application.", ".domain.", ".shared.",
                ".infra.persistence.", ".infra.rest.")) {
            present.invoke(null, classes, layer, violations);
        }
        for (JavaClass source : classes) {
            ArchitectureBoundaryTest.inspectDependencies(source, violations);
            ArchitectureBoundaryTest.inspectEntityMethods(source, violations);
            ArchitectureBoundaryTest.inspectPersistenceObjects(source, violations);
            naming.invoke(null, source, violations);
        }
        int count = new LinkedHashSet<>(violations).size();
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "benchmark-architecture.json"),
                "{\"violationCount\":" + count + "}\n");
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }
}
