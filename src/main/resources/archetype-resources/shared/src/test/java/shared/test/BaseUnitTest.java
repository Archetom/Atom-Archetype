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
 * 单元测试基类
 * <ul>
 *   <li>集成 Mockito</li>
 *   <li>便捷 mock 创建、断言工具</li>
 *   <li>通用测试数据构造方法</li>
 *   <li>增强的异常测试工具</li>
 *   <li>集合和对象断言工具</li>
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

    // ========== Mock 工具 ==========
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

    // ========== 断言工具 ==========
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

    // ========== 异常测试 ==========
    protected void assertThrows(Class<? extends Throwable> expectedType, Runnable action) {
        Assertions.assertThrows(expectedType, action::run);
    }
    protected <T extends Throwable> T assertThrowsWithMessage(
            Class<T> expectedType, String expectedMessage, Runnable action) {
        T exception = Assertions.assertThrows(expectedType, action::run, "异常未抛出");
        Assertions.assertTrue(exception.getMessage() != null && exception.getMessage().contains(expectedMessage),
                "实际异常信息: " + exception.getMessage());
        return exception;
    }
    protected AppException assertAppException(ErrorCodeEnum expectedErrorCode, Runnable action) {
        AppException exception = Assertions.assertThrows(AppException.class, action::run, "未抛出AppException");
        Assertions.assertEquals(expectedErrorCode, exception.getErrorCode());
        return exception;
    }

    // ========== 测试数据构造 ==========
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

    // ========== 生命周期管理 ==========
    protected void initTestData() { /* 可被子类复写 */ }
    protected void clearTestData() { /* 可被子类复写 */ }
}