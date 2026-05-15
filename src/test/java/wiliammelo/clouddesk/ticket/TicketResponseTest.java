package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketResponseTest {

    @Test
    void mapsTicketToResponse() throws Exception {
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
        UUID ticketId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-14T18:00:00Z");
        Instant updatedAt = Instant.parse("2026-05-14T19:00:00Z");

        set(ticket, "id", ticketId);
        set(company, "id", companyId);
        set(customer, "id", customerId);
        set(agent, "id", agentId);
        set(attachment, "id", attachmentId);
        set(attachment, "createdAt", createdAt);
        set(ticket, "createdAt", createdAt);
        set(ticket, "updatedAt", updatedAt);
        ticket.setAssignedAgent(agent);
        ticket.addAttachment(attachment);

        TicketResponse response = TicketResponse.from(ticket);

        assertThat(response.id()).isEqualTo(ticketId);
        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.assignedAgentId()).isEqualTo(agentId);
        assertThat(response.title()).isEqualTo("Printer down");
        assertThat(response.description()).isEqualTo("Need help");
        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(response.attachments()).hasSize(1);
        assertThat(response.attachments().getFirst().id()).isEqualTo(attachmentId);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void mapsTicketWithoutAssignedAgent() throws Exception {
        User owner = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        User customer = new User("Customer", "customer@cloud.test", "hash", UserRole.CUSTOMER);
        Company company = new Company("ByteCare", "bytecare", owner);
        Ticket ticket = new Ticket(company, customer, "Printer down", "Need help", TicketPriority.MEDIUM);
        UUID ticketId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        set(ticket, "id", ticketId);
        set(company, "id", companyId);
        set(customer, "id", customerId);

        TicketResponse response = TicketResponse.from(ticket);

        assertThat(response.assignedAgentId()).isNull();
        assertThat(response.attachments()).isEmpty();
    }

    private void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
