package wiliammelo.clouddesk.owner;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerResponseTest {

    @Test
    void mapsFromUser() {
        User user = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);

        OwnerResponse response = OwnerResponse.from(user);

        assertThat(response.id()).isNull();
        assertThat(response.name()).isEqualTo("Owner");
        assertThat(response.email()).isEqualTo("owner@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.OWNER);
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isNull();
        assertThat(response.updatedAt()).isNull();
    }
}
