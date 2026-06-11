package ${package}.application.service.template;

import ${package}.shared.exception.AppException;
import ${package}.shared.exception.AppUnRetryException;
import ${package}.shared.template.ServiceCallback;
import ${package}.shared.template.ServiceTemplate;
import ${package}.shared.template.chain.TemplateChain;
import ${package}.shared.template.chain.StandardTemplateSteps;
import ${package}.shared.util.ResultUtil;
import ${package}.shared.enums.EventEnum;
import io.github.archetom.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * query service template
 *
 * @author hanfeng
 */
@Slf4j
@Component("queryServiceTemplate")
public class AbstractQueryServiceTemplate implements ServiceTemplate {

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public <T> Result<T> doBiz(EventEnum eventCode, ServiceCallback<T> action) {
        final Result<T> result = new Result<>();

        try {
            // initialize context
            begin();

            // build responsibility chain
            TemplateChain<T> chain = new TemplateChain<T>()
                    .addStep(StandardTemplateSteps.checkParam()) // parameter validation
                    .addStep(StandardTemplateSteps.buildContext()) // build context
                    .addStep(StandardTemplateSteps.process()) // process
                    .addStep(StandardTemplateSteps.after()); // post-processing

            // execute responsibility chain, get process result
            T data = chain.execute(action);

            result.setData(data);
            result.setSuccess(true);
            return result;
        } catch (AppUnRetryException ex) {
            log.error("App Biz Error non-retryable of business exception {}", ex.getMessage(), ex);
            return ResultUtil.genErrorResult(result, ex, eventCode.getCode(), appName);

        } catch (AppException ex) {
            log.error("App Biz Error retryable of business exception {}", ex.getMessage(), ex);
            return ResultUtil.genErrorResult(result, ex, eventCode.getCode(), appName);

        } catch (Throwable ex) {
            log.error("App System Error:{}", ex.getMessage(), ex);
            return ResultUtil.genErrorResult(ex, appName);

        } finally {
            log.info("App result:" + result);
        }
    }
}
