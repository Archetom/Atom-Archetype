package ${package}.domain.repository;

import ${package}.domain.aggregate.AggregateRoot;
import ${package}.domain.specification.Specification;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * base repository interface
 * @author hanfeng
 */
public interface BaseRepository<T extends AggregateRoot<ID>, ID extends Serializable> {

    /**
     * save aggregate root
     */
    T save(T aggregate);

    /**
     * batch save
     */
    List<T> saveAll(List<T> aggregates);

    /**
     * delete aggregate root
     */
    void delete(T aggregate);

    /**
     * based on ID delete
     */
    void deleteById(ID id);

    /**
     * based on ID find
     */
    Optional<T> findById(ID id);

    /**
     * check whether exists
     */
    boolean existsById(ID id);

    /**
     * based on specification find
     */
    List<T> findBySpecification(Specification<T> specification);

    /**
     * based on specification find single
     */
    Optional<T> findOneBySpecification(Specification<T> specification);

    /**
     * based on specification count
     */
    long countBySpecification(Specification<T> specification);
}
