package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.security.JwtPrincipal;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentTicketControllerTest {

    private final TicketService ticketService = mock(TicketService.class);
    private final AgentTicketController controller = new AgentTicketController(ticketService);
    private final JwtPrincipal principal = new JwtPrincipal(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "agent@cloud.test",
            UserRole.AGENT,
            UUID.fromString("22222222-2222-2222-2222-222222222222")
    );
    private final UUID companyId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID ticketId = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void listsAgentTickets() {
        List<TicketResponse> response = List.of(response());
        when(ticketService.listForAgent(principal.userId(), companyId)).thenReturn(response);

        assertThat(controller.list(principal, companyId)).isEqualTo(response);
    }

    @Test
    void getsAgentTicket() {
        TicketResponse response = response();
        when(ticketService.getForAgent(principal.userId(), companyId, ticketId)).thenReturn(response);

        assertThat(controller.get(principal, companyId, ticketId)).isEqualTo(response);
    }

    private TicketResponse response() {
        Instant now = Instant.parse("2026-05-14T18:00:00Z");
        return new TicketResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "Printer down",
                "Need help",
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                List.of(),
                now,
                now
        );
    }
}
