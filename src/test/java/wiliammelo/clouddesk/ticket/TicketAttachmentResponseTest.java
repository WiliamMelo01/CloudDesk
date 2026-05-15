package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketAttachmentResponseTest {

    @Test
    void mapsAttachmentToResponse() throws Exception {
        Ticket ticket = new Ticket(
                new wiliammelo.clouddesk.company.Company("ByteCare", "bytecare", new wiliammelo.clouddesk.user.User("Owner", "owner@cloud.test", "hash", wiliammelo.clouddesk.user.UserRole.OWNER)),
                new wiliammelo.clouddesk.user.User("Customer", "customer@cloud.test", "hash", wiliammelo.clouddesk.user.UserRole.CUSTOMER),
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        );
        TicketAttachment attachment = new TicketAttachment(
                ticket,
                "evidence.png",
                "image/png",
                123L,
                "tickets/1/evidence.png",
                "http://localhost/evidence.png"
        );
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-14T18:00:00Z");
        set(attachment, "id", id);
        set(attachment, "createdAt", createdAt);

        TicketAttachmentResponse response = TicketAttachmentResponse.from(attachment);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.filename()).isEqualTo("evidence.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.contentLength()).isEqualTo(123L);
        assertThat(response.fileUrl()).isEqualTo("http://localhost/evidence.png");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    private void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
