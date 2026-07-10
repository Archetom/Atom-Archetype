package ${package}.infra.persistence.converter;

import ${package}.domain.entity.User;
import ${package}.domain.model.UserStatus;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PasswordHash;
import ${package}.domain.valueobject.PhoneNumber;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;
import ${package}.domain.valueobject.Username;
import ${package}.infra.persistence.mysql.po.UserPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserPOConverterTest {

    private final UserPOConverter converter = Mappers.getMapper(UserPOConverter.class);

    @Test
    void roundTripsAllPersistenceFieldsWithoutRaisingEvents() {
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        LocalDateTime updated = LocalDateTime.now();
        User source = User.reconstitute(
                new UserId(10L),
                new Username("alice"),
                new Email("alice@example.com"),
                new PhoneNumber("+8613800138000"),
                PasswordHash.fromTrustedHash("password-hash"),
                "Alice",
                UserStatus.ACTIVE,
                new TenantId(2L),
                "EXT-10",
                true,
                true,
                7L,
                created,
                updated);

        UserPO po = converter.toPO(source);
        User restored = converter.toDomain(po);

        assertAll(
                () -> assertEquals(10L, po.getId()),
                () -> assertEquals(2L, po.getTenantId()),
                () -> assertEquals("+8613800138000", po.getPhoneNumber()),
                () -> assertEquals("password-hash", po.getPasswordHash()),
                () -> assertFalse(po.toString().contains("password-hash")),
                () -> assertEquals(
                        new UserPO().setPasswordHash("first"),
                        new UserPO().setPasswordHash("second")),
                () -> assertEquals("EXT-10", po.getExternalId()),
                () -> assertTrue(po.getExternalUser()),
                () -> assertTrue(po.getAdmin()),
                () -> assertEquals(7L, po.getVersion()),
                () -> assertEquals(7L, restored.getVersion()),
                () -> assertTrue(restored.getPasswordHash().sameValueAs(
                        PasswordHash.fromTrustedHash("password-hash"))),
                () -> assertEquals(new TenantId(2L), restored.getTenantId()),
                () -> assertEquals(created, restored.getCreatedTime()),
                () -> assertEquals(updated, restored.getUpdatedTime()),
                () -> assertFalse(restored.hasDomainEvents())
        );
    }
}
