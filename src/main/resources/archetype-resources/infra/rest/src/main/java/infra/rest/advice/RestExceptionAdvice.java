package ${package}.infra.rest.advice;

import ${package}.shared.util.ResultUtil;
import ${package}.infra.rest.util.ErrorResultWrapUtil;
import ${package}.infra.rest.util.ResponseEntityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 异常处理
 *
 * @author laiyongguo
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionAdvice {

    @Value("${spring.application.name}")
    private String appName;

    /**
     * 校验错误拦截处理
     *
     * @param exception 错误信息集合
     * @return 错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validationBodyException(MethodArgumentNotValidException exception) {
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResultValidation(exception, appName));
    }

    /**
     * 参数类型转换错误
     *
     * @param exception 错误
     * @return 错误信息
     */
    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<?> parameterTypeException(HttpMessageConversionException exception) {
        log.error("{}App 参数校验，参数转化失败{}", exception.getCause().getLocalizedMessage(), exception);
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResultValidation(exception, appName));
    }

    /**
     * 运行异常
     *
     * @param exception RuntimeException
     * @return ResponseEntity
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtimeException(RuntimeException exception) {
        return ResponseEntityUtil.assembleResponse(ResultUtil.genErrorResult(exception, appName));
    }
}
