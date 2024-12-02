package ${package}.infra.rest.util;

import ${package}.shared.enums.ErrorCodeEnum;
import ${package}.shared.util.ErrorUtil;
import io.github.archetom.common.result.Result;
import io.github.archetom.common.error.ErrorCode;
import io.github.archetom.common.error.ErrorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

/**
 * 结果处理类
 *
 * @author hanfeng
 */
@Slf4j
public class ErrorResultWrapUtil {

    /**
     * 生成 controller 验证错误结果。
     *
     * @param ex      异常
     * @param appName 应用名称
     * @return 错误结果
     */
    public static <T> Result<T> genErrorResultValidation(T ex, String appName) {
        String message = "";
        String fieldName = "";
        if (ex instanceof MethodArgumentNotValidException) {
            BindingResult bindingResult = ((MethodArgumentNotValidException) ex).getBindingResult();
            if (bindingResult.hasErrors()) {
                List<ObjectError> errors = bindingResult.getAllErrors();
                errors.forEach(p -> {
                    FieldError fieldError = (FieldError) p;
                    log.error("Data check failure : object{" + fieldError.getObjectName() + "},field{" + fieldError.getField() +
                            "},errorMessage{" + fieldError.getDefaultMessage() + "}");
                });
                if (!errors.isEmpty()) {
                    FieldError fieldError = (FieldError) errors.get(0);
                    message = fieldError.getDefaultMessage();
                    fieldName = fieldError.getField();
                }
            }
        }

        if (ex instanceof HttpMessageConversionException) {
            message = ((HttpMessageConversionException) ex).getCause().getLocalizedMessage();
        }

        final Result<T> result = new Result<>();
        result.setSuccess(false);

        // 组装参数校验的错误
        ErrorCodeEnum errorCodeEnum = ErrorCodeEnum.PARAM_CHECK_EXP;
        ErrorContext errorContext = ErrorUtil.makeAndAddError(
                new ErrorCode(errorCodeEnum.getCompleteCode("9999"), ErrorCodeEnum.VERSION),
                String.format("%s:%s:%s", errorCodeEnum.getDescription(), fieldName, message), appName);

        result.setErrorContext(errorContext);

        return result;
    }
}
