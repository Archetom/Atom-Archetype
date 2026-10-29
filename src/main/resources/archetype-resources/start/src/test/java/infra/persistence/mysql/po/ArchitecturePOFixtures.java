package ${package}.infra.persistence.mysql.po;

import ${package}.domain.aggregate.AggregateRoot;

import java.util.List;

public final class ArchitecturePOFixtures {
    public static class DetachedPO {
    }

    public static class LeakingPO extends BasePO {
        List<AggregateRoot<String>> aggregates;
    }

    public static class ScalarPO extends BasePO {
        String label;
    }
}
