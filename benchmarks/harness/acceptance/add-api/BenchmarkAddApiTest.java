package com.example.benchmark;

import com.example.benchmark.application.security.AuthenticatedCaller;
import com.example.benchmark.application.service.UserService;
import com.example.benchmark.domain.entity.User;
import com.example.benchmark.domain.repository.UserRepository;
import com.example.benchmark.domain.valueobject.*;
import io.github.archetom.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Evaluator-owned contract; no environment annotation may silently skip these tests. */
@Import(TestTokens.TrustedIssuer.class)
class BenchmarkAddApiTest extends BaseIntegrationTest {
    @Autowired UserRepository repository;
    @Autowired UserService service;

    @Test
    void validEmailPersistsAndReadsBack() throws Exception {
        User user = user(11, "owner", "old@example.com");
        mockMvc.perform(update(user, 11, "users:write", "{\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk());
        assertEquals("new@example.com", storedEmail(user));
        mockMvc.perform(get("/api/v1/users/{id}", user.getId().getValue())
                        .header(AUTHORIZATION, bearer(11, "users:read")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void invalidEmailCannotMutateData() throws Exception {
        User user = user(11, "owner", "old@example.com");
        for (String body : new String[]{"{}", "{\"email\":null}", "{\"email\":\"\"}",
                "{\"email\":\"invalid\"}", "{\"email\":\"" + "a".repeat(250) + "@example.com\"}"}) {
            mockMvc.perform(update(user, 11, "users:write", body)).andExpect(status().isBadRequest());
            assertEquals("old@example.com", storedEmail(user));
        }
    }

    @Test
    void anonymousAndInvalidTokensCannotWrite() throws Exception {
        User user = user(11, "owner", "old@example.com");
        mockMvc.perform(put("/api/v1/users/{id}/email", user.getId().getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"new@example.com\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/users/{id}/email", user.getId().getValue())
                        .header(AUTHORIZATION, "Bearer forged-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"new@example.com\"}"))
                .andExpect(status().isUnauthorized());
        assertEquals("old@example.com", storedEmail(user));
    }

    @Test
    void readAuthorityCannotWrite() throws Exception {
        User user = user(11, "owner", "old@example.com");
        mockMvc.perform(update(user, 11, "users:read", "{\"email\":\"new@example.com\"}"))
                .andExpect(status().isForbidden());
        assertEquals("old@example.com", storedEmail(user));
    }

    @Test
    void callerTenantCannotBeReplacedByRequestData() throws Exception {
        User user = user(11, "owner", "old@example.com");
        mockMvc.perform(update(user, 22, "users:write", "{\"email\":\"new@example.com\"}").param("tenantId", "11")
                        .header("X-Dev-User-Id", "7").header("X-Dev-Tenant-Id", "11"))
                .andExpect(status().isNotFound());
        assertEquals("old@example.com", storedEmail(user));
    }

    @Test
    void applicationUseCaseEnforcesWriteAuthority() throws Exception {
        User user = user(11, "owner", "old@example.com");
        Method operation = assertDoesNotThrow(() -> UserService.class.getMethod(
                "updateUserEmail", AuthenticatedCaller.class, Long.class, String.class));
        Result<?> forbidden = (Result<?>) operation.invoke(service,
                new AuthenticatedCaller(7L, 11L, Set.of("users:read")), user.getId().getValue(), "new@example.com");
        assertFalse(forbidden.isSuccess());
        Result<?> anonymous = (Result<?>) operation.invoke(service, null, user.getId().getValue(), "new@example.com");
        assertFalse(anonymous.isSuccess());
        assertEquals("old@example.com", storedEmail(user));
    }

    @Test
    void emailUniquenessRemainsTenantScoped() throws Exception {
        User user = user(11, "owner", "old@example.com");
        user(11, "peer", "taken@example.com");
        user(22, "other", "other@example.com");
        mockMvc.perform(update(user, 11, "users:write", "{\"email\":\"taken@example.com\"}"))
                .andExpect(status().isConflict());
        assertEquals("old@example.com", storedEmail(user));
        mockMvc.perform(update(user, 11, "users:write", "{\"email\":\"other@example.com\"}"))
                .andExpect(status().isOk());
        assertEquals("other@example.com", storedEmail(user));
    }

    @Test
    void deletedAndMissingUsersCannotBeUpdated() throws Exception {
        User user = user(11, "owner", "old@example.com");
        user.delete();
        repository.save(new TenantId(11L), user);
        mockMvc.perform(update(user, 11, "users:write", "{\"email\":\"new@example.com\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/users/999999/email").header(AUTHORIZATION, bearer(11, "users:write"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"new@example.com\"}"))
                .andExpect(status().isNotFound());
        assertEquals("old@example.com", storedEmail(user));
    }

    private User user(long tenant, String username, String email) {
        TenantId id = new TenantId(tenant);
        return repository.save(id, User.createWithPasswordHash(id, new Username(username), new Email(email),
                PasswordHash.fromTrustedHash("trusted-benchmark-hash"), "Benchmark User"));
    }

    private String storedEmail(User user) {
        return new JdbcTemplate(dataSource).queryForObject("SELECT email FROM t_user WHERE id = ?",
                String.class, user.getId().getValue());
    }

    private MockHttpServletRequestBuilder update(User user, long tenant, String authorities, String body) {
        return put("/api/v1/users/{id}/email", user.getId().getValue())
                .header(AUTHORIZATION, bearer(tenant, authorities)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String bearer(long tenant, String authorities) {
        return "Bearer " + TestTokens.token(7, tenant, authorities);
    }

    @Override protected void initTestData() { truncateTable("t_user"); }
    @Override protected void clearTestData() { truncateTable("t_user"); }
}
