package ${package}.infra.rest.util;

import ${package}.infra.rest.result.RestErrorResult;
import ${package}.shared.enums.ApplicationErrorCode;
import io.github.archetom.common.error.CommonError;
import io.github.archetom.common.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Rest result encapsulate
 *
 * @author hanfeng
 */
public final class ResponseEntityUtil {

    private ResponseEntityUtil() {
    }

    private static <T> ResponseEntity<T> success(Result<T> result) {
        return ResponseEntity.ok(result.getData());
    }

    private static ResponseEntity<?> fail(Result<?> result) {
        CommonError error = result.getErrorContext() == null
                ? null : result.getErrorContext().fetchRootError();

        RestErrorResult restResult = new RestErrorResult();
        if (error == null || error.getErrorCode() == null) {
            ApplicationErrorCode fallback = ApplicationErrorCode.UNKNOWN;
            restResult.setErrCode(fallback.getCompleteCode("9999"));
            restResult.setErrMsg(fallback.getDescription().trim());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(restResult);
        }

        restResult.setErrCode(error.getErrorCode().toString());
        restResult.setErrMsg(error.getErrorMsg());

        return ResponseEntity.status(resolveHttpStatus(error)).body(restResult);
    }

    public static ResponseEntity<?> assembleResponse(Result<?> result) {
        if (result.isSuccess()) {
            return ResponseEntityUtil.success(result);
        } else {
            return ResponseEntityUtil.fail(result);
        }
    }

    private static HttpStatus resolveHttpStatus(CommonError error) {
        ApplicationErrorCode errorCode = ApplicationErrorCode.fromCode(error.getErrorCode().getErrorSpecific());
        return switch (errorCode) {
            case PARAMETER_INVALID -> HttpStatus.BAD_REQUEST;
            case AUTHENTICATION_REQUIRED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VERSION_CONFLICT, RESOURCE_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case UNKNOWN, SYSTEM -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }
}
