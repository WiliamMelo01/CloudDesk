package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class TicketAttachmentTest {

    @Test
    void createsAttachment() {
        User owner = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        User customer = new User("Customer", "customer@cloud.test", "hash", UserRole.CUSTOMER);
        Ticket ticket = new Ticket(
                new Company("ByteCare", "bytecare", owner),
                customer,
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

        attachment.prePersist();

        assertThat(attachment.getId()).isNull();
        assertThat(attachment.getTicket()).isEqualTo(ticket);
        assertThat(attachment.getFilename()).isEqualTo("evidence.png");
        assertThat(attachment.getContentType()).isEqualTo("image/png");
        assertThat(attachment.getContentLength()).isEqualTo(123L);
        assertThat(attachment.getObjectKey()).isEqualTo("tickets/1/evidence.png");
        assertThat(attachment.getFileUrl()).isEqualTo("http://localhost/evidence.png");
        assertThat(attachment.getCreatedAt()).isNotNull();
    }

    @Test
    void supportsProtectedConstructor() throws Exception {
        java.lang.reflect.Constructor<TicketAttachment> constructor = TicketAttachment.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThat(constructor.newInstance()).isNotNull();
    }
}
