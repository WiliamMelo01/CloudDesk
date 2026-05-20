package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMessageAttachmentTest {

    @Test
    void createsMessageAttachment() {
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

        attachment.prePersist();

        assertThat(attachment.getId()).isNull();
        assertThat(attachment.getMessage()).isEqualTo(message);
        assertThat(attachment.getFilename()).isEqualTo("analysis.pdf");
        assertThat(attachment.getContentType()).isEqualTo("application/pdf");
        assertThat(attachment.getContentLength()).isEqualTo(456L);
        assertThat(attachment.getObjectKey()).isEqualTo("tickets/1/messages/1/analysis.pdf");
        assertThat(attachment.getFileUrl()).isEqualTo("http://localhost/analysis.pdf");
        assertThat(attachment.getCreatedAt()).isNotNull();
    }

    @Test
    void supportsProtectedConstructor() throws Exception {
        java.lang.reflect.Constructor<TicketMessageAttachment> constructor = TicketMessageAttachment.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThat(constructor.newInstance()).isNotNull();
    }
}
