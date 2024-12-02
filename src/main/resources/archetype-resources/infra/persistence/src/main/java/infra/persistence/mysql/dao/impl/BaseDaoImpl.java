package ${package}.infra.persistence.mysql.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ${package}.infra.persistence.mysql.dao.BaseDao;
import ${package}.infra.persistence.mysql.mapper.BaseMapper;

/**
 * @author hanfeng
 */
public class BaseDaoImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseDao<T> {

}
