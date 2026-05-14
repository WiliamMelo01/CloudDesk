package wiliammelo.clouddesk.admin;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class AdminResponseTest {

    @Test
    void mapsFromUser() {
        User user = new User("Admin", "admin@cloud.test", "hash", UserRole.ADMIN);

        AdminResponse response = AdminResponse.from(user);

        assertThat(response.id()).isNull();
        assertThat(response.name()).isEqualTo("Admin");
        assertThat(response.email()).isEqualTo("admin@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isNull();
        assertThat(response.updatedAt()).isNull();
    }
}
