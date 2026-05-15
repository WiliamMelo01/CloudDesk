package wiliammelo.clouddesk.agent;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentControllerTest {

    private final AgentService agentService = mock(AgentService.class);
    private final AgentController agentController = new AgentController(agentService);

    @Test
    void createsAgent() {
        AgentCreateRequest request = new AgentCreateRequest("Agent", "agent@cloud.test", "password123");
        AgentResponse response = response();
        when(agentService.create(request)).thenReturn(response);

        assertThat(agentController.create(request)).isEqualTo(response);
    }

    @Test
    void listsAgents() {
        List<AgentResponse> response = List.of(response());
        when(agentService.list()).thenReturn(response);

        assertThat(agentController.list()).isEqualTo(response);
    }

    @Test
    void getsAgent() {
        UUID id = UUID.randomUUID();
        AgentResponse response = response();
        when(agentService.get(id)).thenReturn(response);

        assertThat(agentController.get(id)).isEqualTo(response);
    }

    @Test
    void updatesAgent() {
        UUID id = UUID.randomUUID();
        AgentUpdateRequest request = new AgentUpdateRequest("Updated", "updated@cloud.test", null);
        AgentResponse response = response();
        when(agentService.update(id, request)).thenReturn(response);

        assertThat(agentController.update(id, request)).isEqualTo(response);
    }

    @Test
    void deletesAgent() {
        UUID id = UUID.randomUUID();

        agentController.delete(id);

        verify(agentService).delete(id);
    }

    private AgentResponse response() {
        Instant now = Instant.parse("2026-05-14T18:00:00Z");
        return new AgentResponse(UUID.randomUUID(), "Agent", "agent@cloud.test", UserRole.AGENT, true, now, now);
    }
}
