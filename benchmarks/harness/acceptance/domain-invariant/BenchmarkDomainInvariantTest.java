package com.example.benchmark;

import com.example.benchmark.domain.entity.User;
import com.example.benchmark.domain.event.UserStatusChangedEvent;
import com.example.benchmark.domain.exception.UserDomainException;
import com.example.benchmark.domain.model.UserStatus;
import com.example.benchmark.domain.valueobject.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkDomainInvariantTest {
    @Test
    void lockedUserCannotDirectlyBecomeActive() {
        User user = persisted();
        user.lock("review");
        user.clearDomainEvents();
        var updated = user.getUpdatedTime();
        assertThrows(UserDomainException.class, () -> user.changeStatus(UserStatus.ACTIVE, "skip review"));
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertEquals(updated, user.getUpdatedTime());
        assertTrue(user.getDomainEvents().isEmpty());
    }

    @Test
    void inactiveReviewAllowsActivationAndPreservesEvents() {
        User user = persisted();
        user.lock("review");
        user.clearDomainEvents();
        user.changeStatus(UserStatus.INACTIVE, "review complete");
        user.changeStatus(UserStatus.ACTIVE, "activate");
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(2, user.getDomainEvents().size());
        UserStatusChangedEvent reviewed = (UserStatusChangedEvent) user.getDomainEvents().getFirst();
        UserStatusChangedEvent activated = (UserStatusChangedEvent) user.getDomainEvents().getLast();
        assertEquals(UserStatus.LOCKED, reviewed.getOldStatus());
        assertEquals(UserStatus.INACTIVE, reviewed.getNewStatus());
        assertEquals(UserStatus.INACTIVE, activated.getOldStatus());
        assertEquals(UserStatus.ACTIVE, activated.getNewStatus());
    }

    @Test
    void reconstitutedLockedUserMustFollowTheSameRule() {
        User original = persisted();
        User user = User.reconstitute(new User.UserSnapshot(original.getId(), original.getUsername(),
                original.getEmail(), original.getPhoneNumber(), original.getPasswordHash(), original.getRealName(),
                UserStatus.LOCKED, original.getTenantId(), null, false, false, 4L,
                original.getCreatedTime(), original.getUpdatedTime()));
        assertEquals(4L, user.getVersion());
        assertTrue(user.getDomainEvents().isEmpty());
        assertThrows(UserDomainException.class, () -> user.changeStatus(UserStatus.ACTIVE, "skip review"));
        assertEquals(UserStatus.LOCKED, user.getStatus());
    }

    @Test
    void sameStatusRemainsANoOp() {
        User user = persisted();
        user.lock("review");
        user.clearDomainEvents();
        var updated = user.getUpdatedTime();
        assertDoesNotThrow(() -> user.changeStatus(UserStatus.LOCKED, "already locked"));
        assertEquals(updated, user.getUpdatedTime());
        assertTrue(user.getDomainEvents().isEmpty());
    }

    @Test
    void deletionAndExistingTransitionsRemainProtected() {
        User user = persisted();
        user.changeStatus(UserStatus.INACTIVE, "deactivate");
        user.changeStatus(UserStatus.ACTIVE, "activate");
        assertThrows(UserDomainException.class, () -> user.changeStatus(UserStatus.DELETED, "bypass delete"));
        user.delete();
        assertThrows(UserDomainException.class, () -> user.changeStatus(UserStatus.INACTIVE, "revive"));
        assertEquals(UserStatus.DELETED, user.getStatus());
    }

    private User persisted() {
        User user = User.createWithPasswordHash(new TenantId(11L), new Username("owner"),
                new Email("owner@example.com"), PasswordHash.fromTrustedHash("trusted-benchmark-hash"), "Owner");
        user.onPersisted(new UserId(1L), 0L, user.getCreatedTime(), user.getUpdatedTime());
        user.clearDomainEvents();
        return user;
    }
}
