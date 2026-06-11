package ${package}.infra.persistence.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * user persistence object
 * @author hanfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_user")
public class UserPO extends BasePO {

    /**
     * user ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * username
     */
    @TableField("username")
    private String username;

    /**
     * email
     */
    @TableField("email")
    private String email;

    /**
     * phone number
     */
    @TableField("phone_number")
    private String phoneNumber;

    /**
     * password (encrypted)
     */
    @TableField("password")
    private String password;

    /**
     * real name
     */
    @TableField("real_name")
    private String realName;

    /**
     * status
     */
    @TableField("status")
    private String status;

    /**
     * external system ID
     */
    @TableField("external_id")
    private String externalId;

    /**
     * whether External User
     */
    @TableField("is_external_user")
    private Boolean externalUser;

    /**
     * whether administrator
     */
    @TableField("is_admin")
    private Boolean admin;
}
