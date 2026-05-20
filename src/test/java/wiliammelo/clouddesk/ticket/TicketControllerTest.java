package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import wiliammelo.clouddesk.security.JwtPrincipal;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketControllerTest {

    private final TicketService ticketService = mock(TicketService.class);
    private final TicketController ticketController = new TicketController(ticketService);
    private final JwtPrincipal principal = new JwtPrincipal(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "customer@cloud.test",
            UserRole.CUSTOMER,
            UUID.fromString("22222222-2222-2222-2222-222222222222")
    );

    @Test
    void createsTicket() {
        TicketCreateRequest request = new TicketCreateRequest(UUID.randomUUID(), "Printer down", "Need help", TicketPriority.HIGH);
        List<MockMultipartFile> files = List.of(new MockMultipartFile("files", "evidence.png", "image/png", "img".getBytes()));
        TicketResponse response = response();
        when(ticketService.create(principal.userId(), request, List.copyOf(files))).thenReturn(response);

        assertThat(ticketController.create(principal, request, List.copyOf(files))).isEqualTo(response);
    }

    @Test
    void listsCurrentCustomerTickets() {
        List<TicketResponse> response = List.of(response());
        when(ticketService.list(principal.userId())).thenReturn(response);

        assertThat(ticketController.listCurrentCustomerTickets(principal)).isEqualTo(response);
    }

    @Test
    void getsTicket() {
        UUID id = UUID.randomUUID();
        TicketResponse response = response();
        when(ticketService.get(principal.userId(), id)).thenReturn(response);

        assertThat(ticketController.get(principal, id)).isEqualTo(response);
    }

    @Test
    void repliesToTicket() {
        UUID id = UUID.randomUUID();
        TicketMessageRequest request = new TicketMessageRequest("Ja estamos analisando.");
        List<MockMultipartFile> files = List.of(new MockMultipartFile("files", "evidence.pdf", "application/pdf", "pdf".getBytes()));
        TicketResponse response = response();
        when(ticketService.replyAsCustomer(principal.userId(), id, request, List.copyOf(files))).thenReturn(response);

        assertThat(ticketController.reply(principal, id, request, List.copyOf(files))).isEqualTo(response);
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
