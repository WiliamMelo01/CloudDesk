package wiliammelo.clouddesk.agent;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentResponseTest {

    @Test
    void mapsUserToResponse() {
        User agent = new User("Agent", "agent@cloud.test", "hash", UserRole.AGENT);
        User owner = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        Company company = new Company("Acme", "acme", owner);
        company.addAgent(agent);

        AgentResponse response = AgentResponse.from(agent, List.of(AgentCompanyResponse.from(company)));

        assertThat(response.name()).isEqualTo("Agent");
        assertThat(response.email()).isEqualTo("agent@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.AGENT);
        assertThat(response.active()).isTrue();
        assertThat(response.companies()).singleElement().satisfies(agentCompany -> {
            assertThat(agentCompany.name()).isEqualTo("Acme");
            assertThat(agentCompany.portalSlug()).isEqualTo("acme");
            assertThat(agentCompany.portalPath()).isEqualTo("/portal/acme");
        });
    }
}
