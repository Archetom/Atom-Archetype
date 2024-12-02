package ${package}.infra.rest.util;

import ${package}.infra.rest.result.RestErrorResult;
import io.github.archetom.common.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Rest结果封装
 *
 * @author hanfeng
 */
public class ResponseEntityUtil {
    private static <T> ResponseEntity<T> success(Result<T> result) {
        return new ResponseEntity<>(result.getData(), HttpStatus.OK);
    }

    private static ResponseEntity<?> fail(Result<?> result) {
        RestErrorResult restResult = new RestErrorResult();
        restResult.setErrCode(result.getErrorContext().getErrorStack().get(0).getErrorCode().toString());
        restResult.setErrMsg(result.getErrorContext().getErrorStack().get(0).getErrorMsg());

        return new ResponseEntity<>(restResult, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ResponseEntity<?> assembleResponse(Result<?> result) {
        if (result.isSuccess()) {
            return ResponseEntityUtil.success(result);
        } else {
            return ResponseEntityUtil.fail(result);
        }
    }
}
