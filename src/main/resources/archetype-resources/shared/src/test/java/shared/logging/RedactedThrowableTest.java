package ${package}.shared.logging;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedactedThrowableTest {

    @Test
    void keepsTypesAndStackFramesButDropsMessages() {
        IllegalStateException original = new IllegalStateException(
                "query failed for alice@example.com",
                new RuntimeException("jdbc:mysql://db?password=secret"));

        RedactedThrowable redacted = RedactedThrowable.of(original);
        String rendered = render(redacted);

        assertEquals(IllegalStateException.class.getName(), redacted.getMessage());
        assertArrayEquals(original.getStackTrace(), redacted.getStackTrace());
        assertEquals(RuntimeException.class.getName(), redacted.getCause().getMessage());
        assertTrue(rendered.contains("keepsTypesAndStackFramesButDropsMessages"));
        assertFalse(rendered.contains("alice@example.com"));
        assertFalse(rendered.contains("password=secret"));
    }

    @Test
    void returnsNullWithoutFailure() {
        assertNull(RedactedThrowable.of(null));
    }

    private static String render(Throwable throwable) {
        StringWriter output = new StringWriter();
        throwable.printStackTrace(new PrintWriter(output));
        return output.toString();
    }
}
