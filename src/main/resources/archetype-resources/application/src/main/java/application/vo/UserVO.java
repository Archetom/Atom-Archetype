#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.application.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * user view object
 * @author hanfeng
 */
@Data
@Accessors(chain = true)
public class UserVO {

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
     * phone number
     */
    private String phoneNumber;

    /**
     * masked phone number
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
     * tenant ID
     */
    private Long tenantId;

    /**
     * created time
     */
    private LocalDateTime createdTime;

    /**
     * updated time
     */
    private LocalDateTime updatedTime;
}
