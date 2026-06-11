package ${package}.domain.specification;

/**
 * specification interface
 * @author hanfeng
 */
public interface Specification<T> {

    /**
     * check object whether specification
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * and
     */
    default Specification<T> and(Specification<T> other) {
        return new AndSpecification<>(this, other);
    }

    /**
     * or
     */
    default Specification<T> or(Specification<T> other) {
        return new OrSpecification<>(this, other);
    }

    /**
     * non-
     */
    default Specification<T> not() {
        return new NotSpecification<>(this);
    }
}
