#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户查询请求
 * @author hanfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class UserQueryRequest extends QueryRequest {
    
    /**
     * 用户名
     */
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;
    
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 状态
     */
    private String status;
}
