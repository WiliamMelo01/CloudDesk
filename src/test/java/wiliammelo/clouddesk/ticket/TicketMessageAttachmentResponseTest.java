package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMessageAttachmentResponseTest {

    @Test
    void mapsMessageAttachmentToResponse() throws Exception {
        User owner = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        User customer = new User("Customer", "customer@cloud.test", "hash", UserRole.CUSTOMER);
        User agent = new User("Agent", "agent@cloud.test", "hash", UserRole.AGENT);
        Ticket ticket = new Ticket(new Company("ByteCare", "bytecare", owner), customer, "Printer down", "Need help", TicketPriority.HIGH);
        TicketMessage message = new TicketMessage(ticket, agent, "Ja estamos analisando.");
        TicketMessageAttachment attachment = new TicketMessageAttachment(
                message,
                "analysis.pdf",
                "application/pdf",
                456L,
                "tickets/1/messages/1/analysis.pdf",
                "http://localhost/analysis.pdf"
        );
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-14T18:00:00Z");
        set(attachment, "id", id);
        set(attachment, "createdAt", createdAt);

        TicketMessageAttachmentResponse response = TicketMessageAttachmentResponse.from(attachment);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.filename()).isEqualTo("analysis.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.contentLength()).isEqualTo(456L);
        assertThat(response.fileUrl()).isEqualTo("http://localhost/analysis.pdf");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    private void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
