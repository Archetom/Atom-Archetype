package ${package}.infra.rest.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author hanfeng
 */
@Data
public class RestErrorResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 2356702836088252228L;

    /**
     * 错误码
     * ErrorEnum中的code
     */
    private String errCode;

    /**
     * 错误信息
     * 可以自定义message以覆盖ErrorEnum中的message
     */
    private String errMsg;
}
