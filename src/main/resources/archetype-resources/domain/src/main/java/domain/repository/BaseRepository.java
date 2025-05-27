package ${package}.domain.repository;

import ${package}.domain.aggregate.AggregateRoot;
import ${package}.domain.specification.Specification;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * 基础仓储接口
 * @author hanfeng
 */
public interface BaseRepository<T extends AggregateRoot<ID>, ID extends Serializable> {

    /**
     * 保存聚合根
     */
    T save(T aggregate);

    /**
     * 批量保存
     */
    List<T> saveAll(List<T> aggregates);

    /**
     * 删除聚合根
     */
    void delete(T aggregate);

    /**
     * 根据ID删除
     */
    void deleteById(ID id);

    /**
     * 根据ID查找
     */
    Optional<T> findById(ID id);

    /**
     * 检查是否存在
     */
    boolean existsById(ID id);

    /**
     * 根据规约查找
     */
    List<T> findBySpecification(Specification<T> specification);

    /**
     * 根据规约查找单个
     */
    Optional<T> findOneBySpecification(Specification<T> specification);

    /**
     * 根据规约统计数量
     */
    long countBySpecification(Specification<T> specification);
}
