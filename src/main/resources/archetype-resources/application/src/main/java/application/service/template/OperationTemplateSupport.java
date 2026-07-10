package ${package}.application.service.template;

import ${package}.application.exception.DomainExceptionMapper;
import ${package}.domain.exception.DomainException;
import ${package}.shared.enums.ApplicationErrorCode;
import ${package}.shared.exception.ApplicationException;
import ${package}.shared.exception.NonRetryableApplicationException;
import ${package}.shared.operation.OperationCode;
import ${package}.shared.util.ResultUtil;
import io.github.archetom.common.result.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Shared result and exception policy for application operation templates.
 */
@Slf4j
abstract class OperationTemplateSupport {

    private final String appName;

    protected OperationTemplateSupport(String appName) {
        this.appName = Objects.requireNonNull(appName, "appName must not be null");
    }

    public final <T> Result<T> execute(OperationCode event, ServiceOperation<T> operation) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(operation, "operation must not be null");

        Result<T> result = new Result<>();
        try {
            operation.validate();
            operation.prepare();
            T data = operation.execute();
            operation.onSuccess(data);
            result.setData(data);
            result.setSuccess(true);
            return result;
        } catch (DomainException exception) {
            onFailure();
            log.warn("Domain operation rejected: event={}, error={}", event, exception.getError());
            NonRetryableApplicationException mapped = new NonRetryableApplicationException(
                    DomainExceptionMapper.toApplicationCode(exception),
                    exception.getMessage(),
                    exception);
            return ResultUtil.genErrorResult(result, mapped, event.code(), appName);
        } catch (IllegalArgumentException exception) {
            onFailure();
            log.warn("Application input rejected: event={}, exceptionType={}",
                    event, exception.getClass().getName());
            NonRetryableApplicationException mapped = new NonRetryableApplicationException(
                    ApplicationErrorCode.PARAMETER_INVALID,
                    ApplicationErrorCode.PARAMETER_INVALID.getDescription(),
                    exception);
            return ResultUtil.genErrorResult(result, mapped, event.code(), appName);
        } catch (NonRetryableApplicationException exception) {
            onFailure();
            log.warn("Application operation rejected: event={}, error={}",
                    event, exception.getErrorCode());
            return ResultUtil.genErrorResult(result, exception, event.code(), appName);
        } catch (ApplicationException exception) {
            onFailure();
            log.warn("Application operation failed: event={}, error={}",
                    event, exception.getErrorCode());
            return ResultUtil.genErrorResult(result, exception, event.code(), appName);
        } catch (RuntimeException exception) {
            onFailure();
            log.error("Unexpected application failure: event={}, exceptionType={}",
                    event, exception.getClass().getName());
            return ResultUtil.genErrorResult(exception, appName);
        } finally {
            log.info("Application operation completed: event={}, success={}",
                    event, result.isSuccess());
        }
    }

    /**
     * Hook used by command execution to mark an active transaction rollback-only.
     */
    protected void onFailure() {
    }
}
