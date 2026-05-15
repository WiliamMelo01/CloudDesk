package wiliammelo.clouddesk.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void createsAndMutatesUser() {
        User user = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);

        user.prePersist();

        assertThat(user.getId()).isNull();
        assertThat(user.getName()).isEqualTo("Owner");
        assertThat(user.getEmail()).isEqualTo("owner@cloud.test");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo(UserRole.OWNER);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getManagedCompanies()).isEmpty();
        assertThat(user.getCompanies()).isEmpty();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();

        user.setName("Updated");
        user.setEmail("updated@cloud.test");
        user.setPasswordHash("newHash");
        user.preUpdate();

        assertThat(user.getName()).isEqualTo("Updated");
        assertThat(user.getEmail()).isEqualTo("updated@cloud.test");
        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(user.getCreatedAt());

        user.deactivate();

        assertThat(user.isActive()).isFalse();
    }
}
