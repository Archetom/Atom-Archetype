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
 * user query request
 * @author hanfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class UserQueryRequest extends QueryRequest {
    
    /**
     * username
     */
    @Size(max = 50, message = " username length cannot 50 characters ")
    private String username;
    
    /**
     * email
     */
    @Email(message = "Invalid email format")
    private String email;
    
    /**
     * status
     */
    private String status;
}
