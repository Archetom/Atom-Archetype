package ${package}.api.dto.response;

import ${package}.domain.aggregate.AggregateRoot;
import ${package}.infra.persistence.mysql.po.BasePO;

import java.util.List;

/** Generic signatures must not hide domain or persistence types from the API rule. */
public final class ArchitectureResponseFixtures {
    public record LeakingDomainResponse(List<AggregateRoot<String>> items) {
    }

    public record LeakingPersistenceResponse(List<BasePO> items) {
    }

    public record ScalarResponse(List<String> items) {
    }
}
