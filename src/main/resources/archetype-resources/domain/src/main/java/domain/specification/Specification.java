package ${package}.domain.specification;

/**
 * 规约接口
 * @author hanfeng
 */
public interface Specification<T> {

    /**
     * 检查对象是否满足规约
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * 与操作
     */
    default Specification<T> and(Specification<T> other) {
        return new AndSpecification<>(this, other);
    }

    /**
     * 或操作
     */
    default Specification<T> or(Specification<T> other) {
        return new OrSpecification<>(this, other);
    }

    /**
     * 非操作
     */
    default Specification<T> not() {
        return new NotSpecification<>(this);
    }
}
