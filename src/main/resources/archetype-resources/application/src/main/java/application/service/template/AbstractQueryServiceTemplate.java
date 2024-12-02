package ${package}.application.template;

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
 * 查询服务模板
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
            // 初始化上下文
            begin();

            // 构建责任链
            TemplateChain<T> chain = new TemplateChain<T>()
                    .addStep(StandardTemplateSteps.checkParam())    // 参数校验
                    .addStep(StandardTemplateSteps.buildContext()) // 构建上下文
                    .addStep(StandardTemplateSteps.process())      // 核心逻辑处理
                    .addStep(StandardTemplateSteps.after());       // 后置处理

            // 执行责任链
            chain.execute(action);

            result.setSuccess(true);
            return result;
        } catch (AppUnRetryException ex) {
            log.error("App Biz Error 无需重试的业务异常{}", ex.getMessage(), ex);
            return ResultUtil.genErrorResult(result, ex, eventCode.getCode(), appName);

        } catch (AppException ex) {
            log.error("App Biz Error 可重试的业务异常{}", ex.getMessage(), ex);
            return ResultUtil.genErrorResult(result, ex, eventCode.getCode(), appName);

        } catch (Throwable ex) {
            log.error("App System Error:{}", ex.getMessage(), ex);
            return ResultUtil.genErrorResult(ex, appName);

        } finally {
            log.info("App操作结果:" + result);
        }
    }
}
