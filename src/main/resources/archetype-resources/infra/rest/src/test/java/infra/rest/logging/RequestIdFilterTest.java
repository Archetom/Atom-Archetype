package ${package}.infra.rest.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void reusesWellFormedGatewayIdForResponseAndLogs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "gateway-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> loggedId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> loggedId.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertEquals("gateway-123", response.getHeader(RequestIdFilter.HEADER));
        assertEquals("gateway-123", loggedId.get());
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }

    @Test
    void replacesUnsafeIdSoCallersCannotForgeLogLines() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc\nERROR forged entry");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(RequestIdFilter.HEADER);
        assertNotNull(requestId);
        assertFalse(requestId.contains("forged"));
        assertDoesNotThrow(() -> UUID.fromString(requestId));
    }

    @Test
    void generatesIdWhenAbsent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertDoesNotThrow(() -> UUID.fromString(response.getHeader(RequestIdFilter.HEADER)));
    }
}
