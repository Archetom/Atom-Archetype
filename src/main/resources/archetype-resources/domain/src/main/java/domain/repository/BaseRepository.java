package ${package}.domain.repository;

import java.io.Serializable;

/**
 * @author hanfeng
 */
public interface BaseRepository<T, ID extends Serializable> {
    /**
     * 保存
     *
     * @param entity 领域模型
     */
    T save(T entity);

    /**
     * 删除
     *
     * @param entity 领域模型
     */
    void delete(T entity);

    /**
     * 删除
     *
     * @param id 领域ID
     */
    void delete(Long id);

    /**
     * 获取领域实例
     *
     * @param id 领域ID
     * @return 领域模型
     */
    T findById(ID id);
}
