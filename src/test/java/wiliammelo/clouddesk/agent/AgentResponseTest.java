package wiliammelo.clouddesk.agent;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class AgentResponseTest {

    @Test
    void mapsUserToResponse() {
        User agent = new User("Agent", "agent@cloud.test", "hash", UserRole.AGENT);

        AgentResponse response = AgentResponse.from(agent);

        assertThat(response.name()).isEqualTo("Agent");
        assertThat(response.email()).isEqualTo("agent@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.AGENT);
        assertThat(response.active()).isTrue();
    }
}
