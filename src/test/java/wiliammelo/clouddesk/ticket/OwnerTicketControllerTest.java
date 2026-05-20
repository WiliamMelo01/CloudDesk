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

class OwnerTicketControllerTest {

    private final TicketService ticketService = mock(TicketService.class);
    private final OwnerTicketController controller = new OwnerTicketController(ticketService);
    private final JwtPrincipal principal = new JwtPrincipal(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "owner@cloud.test",
            UserRole.OWNER,
            UUID.fromString("22222222-2222-2222-2222-222222222222")
    );
    private final UUID companyId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID ticketId = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void listsOwnerTickets() {
        List<TicketResponse> response = List.of(response());
        when(ticketService.listForOwner(principal.userId(), companyId)).thenReturn(response);

        assertThat(controller.list(principal, companyId)).isEqualTo(response);
    }

    @Test
    void getsOwnerTicket() {
        TicketResponse response = response();
        when(ticketService.getForOwner(principal.userId(), companyId, ticketId)).thenReturn(response);

        assertThat(controller.get(principal, companyId, ticketId)).isEqualTo(response);
    }

    @Test
    void repliesToOwnerTicket() {
        TicketMessageRequest request = new TicketMessageRequest("Vamos seguir com a tratativa.");
        java.util.List<org.springframework.mock.web.MockMultipartFile> files = java.util.List.of(
                new org.springframework.mock.web.MockMultipartFile("files", "evidence.pdf", "application/pdf", "pdf".getBytes())
        );
        TicketResponse response = response();
        when(ticketService.replyAsOwner(principal.userId(), companyId, ticketId, request, List.copyOf(files))).thenReturn(response);

        assertThat(controller.reply(principal, companyId, ticketId, request, List.copyOf(files))).isEqualTo(response);
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
                List.of(),
                now,
                now
        );
    }
}
