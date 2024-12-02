package ${package}.infra.persistence.mysql.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author hanfeng
 */
@Data
public class BasePO implements Serializable {
    @Serial
    private static final long serialVersionUID = -7330573630298592450L;

    /**
     * 租户，指 SaaS 租户
     */
    @TableField(fill = FieldFill.INSERT, value = "tenant_id")
    private Long tenantId;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 最后修改时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 删除时间，逻辑删除
     */
    @TableLogic
    @TableField("deleted_time")
    private LocalDateTime deletedTime;
}
