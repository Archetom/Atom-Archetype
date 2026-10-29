package ${package};

import ${package}.api.dto.response.ArchitectureResponseFixtures;
import ${package}.domain.architecturefixture.DomainFixtures;
import ${package}.infra.persistence.mysql.po.ArchitecturePOFixtures;
import ${package}.infra.rest.endpoint.ArchitectureControllerFixtures;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves that the bytecode guards reject defects and accept supported designs. */
class ArchitectureBoundaryRulesTest {
    @Test
    void excludesTestFixturesButKeepsProductionInsideTheArchetypeHarness() {
        String project = "file:/repo/target/test-classes/projects/basic/project/";
        assertTrue(ArchitectureBoundaryTest.isProductionLocation(Location.of(URI.create(
                project + "domain/target/classes/example/domain/Entity.class"))));
        assertFalse(ArchitectureBoundaryTest.isProductionLocation(Location.of(URI.create(
                project + "start/target/test-classes/example/Fixture.class"))));
    }

    @Test
    void rejectsDirectHttpDatabaseAndRedisAccessFromDomain() {
        assertDependencyViolation(DomainFixtures.HttpDependency.class, "java.net.http.HttpClient");
        assertDependencyViolation(DomainFixtures.DatabaseDependency.class, "java.sql.Connection");
        assertDependencyViolation(DomainFixtures.RedisDependency.class, "io.lettuce.core.RedisClient");
    }

    @Test
    void rejectsDomainAndPersistenceTypesInsideGenericApiContracts() {
        assertDependencyViolation(ArchitectureResponseFixtures.LeakingDomainResponse.class, "AggregateRoot");
        assertDependencyViolation(ArchitectureResponseFixtures.LeakingPersistenceResponse.class, "BasePO");
        List<String> violations = new ArrayList<>();
        ArchitectureBoundaryTest.inspectDependencies(importClass(ArchitectureResponseFixtures.ScalarResponse.class), violations);
        assertTrue(violations.isEmpty(), violations::toString);
    }

    @Test
    void recognizesAnnotatedControllersOutsideTheConventionalPackage() {
        assertDependencyViolation(ArchitectureControllerFixtures.LeakingController.class,
                "REST controller bypasses facade/use-case boundary");
    }

    @Test
    void rejectsInheritedAggregateSettersAndAllowsBusinessMethods() {
        List<String> violations = new ArrayList<>();
        ArchitectureBoundaryTest.inspectEntityMethods(importClass(DomainFixtures.InheritedSetterAggregate.class), violations);
        assertTrue(violations.stream().anyMatch(message -> message.contains("setState")), violations::toString);
        violations.clear();
        ArchitectureBoundaryTest.inspectEntityMethods(importClass(DomainFixtures.BusinessAggregate.class), violations);
        assertTrue(violations.isEmpty(), violations::toString);
    }

    @Test
    void persistenceObjectsKeepBaseFieldsAndScalarState() {
        List<String> violations = new ArrayList<>();
        ArchitectureBoundaryTest.inspectPersistenceObjects(importClass(ArchitecturePOFixtures.DetachedPO.class), violations);
        assertTrue(violations.stream().anyMatch(message -> message.contains("must extend BasePO")), violations::toString);
        violations.clear();
        ArchitectureBoundaryTest.inspectPersistenceObjects(importClass(ArchitecturePOFixtures.LeakingPO.class), violations);
        assertTrue(violations.stream().anyMatch(message -> message.contains("leaks domain type")), violations::toString);
        violations.clear();
        ArchitectureBoundaryTest.inspectPersistenceObjects(importClass(ArchitecturePOFixtures.ScalarPO.class), violations);
        assertTrue(violations.isEmpty(), violations::toString);
    }

    private static void assertDependencyViolation(Class<?> fixture, String detail) {
        List<String> violations = new ArrayList<>();
        ArchitectureBoundaryTest.inspectDependencies(importClass(fixture), violations);
        assertTrue(violations.stream().anyMatch(message -> message.contains(detail)), violations::toString);
    }

    private static JavaClass importClass(Class<?> fixture) {
        return new ClassFileImporter().importClasses(fixture).get(fixture);
    }
}
