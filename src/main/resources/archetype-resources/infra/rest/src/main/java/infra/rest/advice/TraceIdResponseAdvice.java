package ${package}.infra.rest.advice;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * add TraceId to response
 * @author hanfeng
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class TraceIdResponseAdvice implements ResponseBodyAdvice<Object> {

    private final Tracer tracer;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true; // for all response
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // add TraceId to response
        String traceId = getTraceId();
        if (traceId != null) {
            response.getHeaders().add("X-Trace-Id", traceId);
            log.debug("ResponseAdvice set TraceId to response: {}", traceId);
        }

        String spanId = getSpanId();
        if (spanId != null) {
            response.getHeaders().add("X-Span-Id", spanId);
        }

        return body;
    }

    private String getTraceId() {
        try {
            if (tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
                return tracer.currentSpan().context().traceId();
            }
        } catch (Exception e) {
            log.warn(" get TraceId failure ", e);
        }
        return null;
    }

    private String getSpanId() {
        try {
            if (tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
                return tracer.currentSpan().context().spanId();
            }
        } catch (Exception e) {
            log.warn(" get SpanId failure ", e);
        }
        return null;
    }
}
