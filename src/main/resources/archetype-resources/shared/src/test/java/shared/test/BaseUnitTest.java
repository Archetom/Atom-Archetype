package ${package}.shared.test;

import ${package}.shared.enums.ErrorCodeEnum;
import ${package}.shared.exception.AppException;
import io.github.archetom.common.result.Result;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static org.mockito.Mockito.*;

/**
 * unit test class
 * <ul>
 * <li> integration Mockito</li>
 * <li> convenience mock create, utility </li>
 * <li> common test data method </li>
 * <li> of exception test utility </li>
 * <li> and object utility </li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {

    private static final Random RANDOM = new Random();

    @BeforeEach
    protected void setUpBase() {
        initTestData();
    }

    @AfterEach
    protected void tearDownBase() {
        clearTestData();
    }

    // ========== Mock utility ==========
    protected <T> T mock(Class<T> clazz) {
        return mock(clazz);
    }
    protected <T> T spy(T object) {
        return spy(object);
    }
    protected <T> void verifyInvoked(T mock, int times) {
        verify(mock, times(times));
    }
    protected <T> void verifyNeverInvoked(T mock) {
        verifyNoInteractions(mock);
    }

    // ========== utility ==========
    protected <T> void assertListNotEmpty(List<T> list) {
        Assertions.assertNotNull(list, "list should not be null");
        Assertions.assertFalse(list.isEmpty(), "list should not be empty");
    }
    protected <T> void assertListSize(List<T> list, int expectedSize) {
        Assertions.assertNotNull(list, "list should not be null");
        Assertions.assertEquals(expectedSize, list.size());
    }
    protected void assertStringNotBlank(String str, String message) {
        Assertions.assertNotNull(str, message + " should not be null");
        Assertions.assertFalse(str.trim().isEmpty(), message + " should not be blank");
    }

    // ========== exception test ==========
    protected void assertThrows(Class<? extends Throwable> expectedType, Runnable action) {
        Assertions.assertThrows(expectedType, action::run);
    }
    protected <T extends Throwable> T assertThrowsWithMessage(
            Class<T> expectedType, String expectedMessage, Runnable action) {
        T exception = Assertions.assertThrows(expectedType, action::run, " exception not ");
        Assertions.assertTrue(exception.getMessage() != null && exception.getMessage().contains(expectedMessage),
                " actual exception: " + exception.getMessage());
        return exception;
    }
    protected AppException assertAppException(ErrorCodeEnum expectedErrorCode, Runnable action) {
        AppException exception = Assertions.assertThrows(AppException.class, action::run, " not AppException");
        Assertions.assertEquals(expectedErrorCode, exception.getErrorCode());
        return exception;
    }

    // ========== test data ==========
    protected String randomString(int length) {
        return RandomStringUtils.randomAlphabetic(length);
    }
    protected Long randomLong(long bound) {
        return Math.abs(RANDOM.nextLong()) % (bound == 0 ? 10000L : bound);
    }
    protected Integer randomInt(int bound) {
        return RANDOM.nextInt(bound);
    }
    protected LocalDateTime randomDateTime() {
        return LocalDateTime.now().minusDays(RANDOM.nextInt(365));
    }
    protected <T> Result<T> createSuccessResult(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    // ========== ==========
    protected void initTestData() {/* can class */}
    protected void clearTestData() {/* can class */}
}