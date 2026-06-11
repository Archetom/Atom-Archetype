#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.dto.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * user response
 * @author hanfeng
 */
@Data
@Accessors(chain = true)
public class UserResponse {

    /**
     * user ID
     */
    private Long id;

    /**
     * username
     */
    private String username;

    /**
     * email
     */
    private String email;

    /**
     * phone number (masked)
     */
    private String maskedPhoneNumber;

    /**
     * real name
     */
    private String realName;

    /**
     * status
     */
    private String status;

    /**
     * status
     */
    private String statusName;

    /**
     * whether active
     */
    private Boolean active;

    /**
     * created time
     */
    private LocalDateTime createdTime;

    /**
     * updated time
     */
    private LocalDateTime updatedTime;
}
