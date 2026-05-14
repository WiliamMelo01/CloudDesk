package wiliammelo.clouddesk.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void createsAndMutatesUser() {
        User user = new User("Admin", "admin@cloud.test", "hash", UserRole.ADMIN);

        user.prePersist();

        assertThat(user.getId()).isNull();
        assertThat(user.getName()).isEqualTo("Admin");
        assertThat(user.getEmail()).isEqualTo("admin@cloud.test");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.isActive()).isTrue();
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
