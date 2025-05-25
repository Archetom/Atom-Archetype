#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.persistence.mysql.mapper;

import ${package}.infra.persistence.mysql.po.UserPO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户Mapper
 * @author hanfeng
 */
@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
    
    /**
     * 分页查询用户
     */
    IPage<UserPO> selectUserPage(
        Page<UserPO> page,
        @Param("username") String username,
        @Param("email") String email,
        @Param("status") String status
    );
}
