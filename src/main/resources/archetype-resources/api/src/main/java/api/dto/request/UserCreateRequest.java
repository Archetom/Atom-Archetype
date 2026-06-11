#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.dto.request;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * user create request
 * @author hanfeng
 */
@Data
@Accessors(chain = true)
public class UserCreateRequest {

    /**
     * username
     */
    @NotBlank(message = "Username must not be empty")
    @Size(min = 3, max = 50, message = "Username length must be between3-50 characters ")
    private String username;

    /**
     * email
     */
    @NotBlank(message = "Email must not be empty")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * phone number
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phoneNumber;

    /**
     * password
     */
    @NotBlank(message = "Password must not be empty")
    @Size(min = 6, max = 20, message = "Password length must be between6-20 characters ")
    private String password;

    /**
     * real name
     */
    @Size(max = 100, message = "Real name length must not exceed100 characters ")
    private String realName;
}
