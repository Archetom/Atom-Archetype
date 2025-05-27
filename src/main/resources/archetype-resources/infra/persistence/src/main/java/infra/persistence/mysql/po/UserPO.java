package ${package}.infra.persistence.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户持久化对象
 * @author hanfeng
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("t_user")
public class UserPO extends BasePO {

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 手机号
     */
    @TableField("phone_number")
    private String phoneNumber;

    /**
     * 密码（加密后）
     */
    @TableField("password")
    private String password;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 状态
     */
    @TableField("status")
    private String status;

    /**
     * 外部系统ID
     */
    @TableField("external_id")
    private String externalId;

    /**
     * 是否外部用户
     */
    @TableField("is_external_user")
    private Boolean externalUser;

    /**
     * 是否管理员
     */
    @TableField("is_admin")
    private Boolean admin;
}
