package ${package}.application.template;

import ${package}.shared.exception.AppException;
import ${package}.shared.exception.AppUnRetryException;
import ${package}.shared.template.ServiceCallback;
import ${package}.shared.template.ServiceTemplate;
import ${package}.shared.template.chain.TemplateChain;
import ${package}.shared.template.chain.StandardTemplateSteps;
import ${package}.shared.util.ResultUtil;
import ${package}.shared.enums.EventEnum;
import ${package}.shared.util.ResultUtil;
import io.github.archetom.common.utils.Profiler;
import io.github.archetom.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * 操作类服务模板
 *
 * @author hanfeng
 */
@Slf4j
@Component("operatorServiceTemplate")
public class AbstractOperatorServiceTemplate implements ServiceTemplate {

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public <T> Result<T> doBiz(EventEnum eventCode, ServiceCallback<T> action) {
        final Result<T> result = new Result<>();
        try {
            begin();

            TemplateChain<T> chain = new TemplateChain<T>()
                    .addStep(StandardTemplateSteps.checkParam())
                    .addStep(StandardTemplateSteps.buildContext())
                    .addStep(StandardTemplateSteps.checkConcurrent())
                    .addStep(StandardTemplateSteps.process())
                    .addStep(StandardTemplateSteps.persistence())
                    .addStep(StandardTemplateSteps.after());

            chain.execute(action);

            result.setSuccess(true);
            return result;

        } catch (AppUnRetryException ex) {
            log.error("App Biz Error 无需重试的业务异常{}", ex.getMessage(), ex);
            try {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            } catch (NoTransactionException exception) {
                return ResultUtil.genErrorResult(result, ex, eventCode.getCode(), appName);
            }
            return ResultUtil.genErrorResult(result, ex, eventCode.getCode(), appName);

        } catch (AppException ex) {
            log.error("App Biz Error 可重试的业务异常{}", ex.getMessage(), ex);
            try {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            } catch (NoTransactionException exception) {
                return ResultUtil.genErrorResult(result, ex, eventCode.getCode(), appName);
            }
            return ResultUtil.genErrorResult(result, ex, eventCode.getCode(), appName);

        } catch (Throwable ex) {
            log.error("App System Error:{}", ex.getMessage(), ex);
            try {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            } catch (NoTransactionException exception) {
                return ResultUtil.genErrorResult(ex, appName);
            }
            return ResultUtil.genErrorResult(ex, appName);

        } finally {
            log.info("App操作结果:" + result);
        }
    }
}
