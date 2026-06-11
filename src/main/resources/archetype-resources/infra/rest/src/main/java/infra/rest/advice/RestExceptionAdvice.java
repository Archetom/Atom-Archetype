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
 * exception process
 *
 * @author laiyongguo
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionAdvice {

    @Value("${spring.application.name}")
    private String appName;

    /**
     * error process
     *
     * @param exception error message
     * @return error message
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validationBodyException(MethodArgumentNotValidException exception) {
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResultValidation(exception, appName));
    }

    /**
     * parameter class convert error
     *
     * @param exception error
     * @return error message
     */
    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<?> parameterTypeException(HttpMessageConversionException exception) {
        log.error("{}App parameter validation, parameter failure {}", exception.getCause().getLocalizedMessage(), exception);
        return ResponseEntityUtil.assembleResponse(ErrorResultWrapUtil.genErrorResultValidation(exception, appName));
    }

    /**
     * exception
     *
     * @param exception RuntimeException
     * @return ResponseEntity
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtimeException(RuntimeException exception) {
        return ResponseEntityUtil.assembleResponse(ResultUtil.genErrorResult(exception, appName));
    }
}
