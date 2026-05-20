package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTest {

    @Test
    void managesTicketState() throws Exception {
        User owner = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        User customer = new User("Customer", "customer@cloud.test", "hash", UserRole.CUSTOMER);
        User agent = new User("Agent", "agent@cloud.test", "hash", UserRole.AGENT);
        Company company = new Company("ByteCare", "bytecare", owner);
        Ticket ticket = new Ticket(company, customer, "Printer down", "Need help", TicketPriority.HIGH);
        TicketAttachment attachment = new TicketAttachment(
                ticket,
                "evidence.png",
                "image/png",
                123L,
                "tickets/1/evidence.png",
                "http://localhost/evidence.png"
        );
        TicketMessage message = new TicketMessage(ticket, agent, "Ja estamos analisando.");

        assertThat(ticket.getId()).isNull();
        assertThat(ticket.getCompany()).isEqualTo(company);
        assertThat(ticket.getCustomer()).isEqualTo(customer);
        assertThat(ticket.getAssignedAgent()).isNull();
        assertThat(ticket.getTitle()).isEqualTo("Printer down");
        assertThat(ticket.getDescription()).isEqualTo("Need help");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(ticket.getAttachments()).isEmpty();
        assertThat(ticket.getMessages()).isEmpty();

        ticket.setAssignedAgent(agent);
        ticket.addAttachment(attachment);
        ticket.addMessage(message);
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        assertThat(ticket.getAssignedAgent()).isEqualTo(agent);
        assertThat(ticket.getAttachments()).containsExactly(attachment);
        assertThat(ticket.getMessages()).containsExactly(message);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        ticket.prePersist();
        Instant createdAt = ticket.getCreatedAt();
        Instant updatedAt = ticket.getUpdatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(updatedAt).isNotNull();

        Thread.sleep(1);
        ticket.preUpdate();

        assertThat(ticket.getCreatedAt()).isEqualTo(createdAt);
        assertThat(ticket.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
    }

    @Test
    void supportsProtectedConstructor() throws Exception {
        java.lang.reflect.Constructor<Ticket> constructor = Ticket.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThat(constructor.newInstance()).isNotNull();
    }
}
