package ${package}.infra.rest.interceptor;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * TraceId 响应头拦截器
 * @author hanfeng
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceIdInterceptor implements HandlerInterceptor {

    private final Tracer tracer;

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = getTraceId();
        String spanId = getSpanId();

        if (traceId != null) {
            response.setHeader(TRACE_ID_HEADER, traceId);
            log.debug("设置TraceId到响应头: {}", traceId);
        }

        if (spanId != null) {
            response.setHeader(SPAN_ID_HEADER, spanId);
            log.debug("设置SpanId到响应头: {}", spanId);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String traceId = getTraceId();
        if (traceId != null && response.getHeader(TRACE_ID_HEADER) == null) {
            response.setHeader(TRACE_ID_HEADER, traceId);
        }
    }

    private String getTraceId() {
        try {
            if (tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
                return tracer.currentSpan().context().traceId();
            }
        } catch (Exception e) {
            log.warn("获取TraceId失败", e);
        }
        return null;
    }

    private String getSpanId() {
        try {
            if (tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
                return tracer.currentSpan().context().spanId();
            }
        } catch (Exception e) {
            log.warn("获取SpanId失败", e);
        }
        return null;
    }
}
