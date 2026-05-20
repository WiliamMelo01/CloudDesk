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
        TicketMessage message = new TicketMessage(ticket, agent, "Ja estamos analisando.");
        TicketMessageAttachment messageAttachment = new TicketMessageAttachment(
                message,
                "analysis.pdf",
                "application/pdf",
                456L,
                "tickets/1/messages/1/analysis.pdf",
                "http://localhost/analysis.pdf"
        );
        UUID ticketId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID messageAttachmentId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-14T18:00:00Z");
        Instant updatedAt = Instant.parse("2026-05-14T19:00:00Z");

        set(ticket, "id", ticketId);
        set(company, "id", companyId);
        set(customer, "id", customerId);
        set(agent, "id", agentId);
        set(attachment, "id", attachmentId);
        set(attachment, "createdAt", createdAt);
        set(message, "id", messageId);
        set(message, "createdAt", updatedAt);
        set(messageAttachment, "id", messageAttachmentId);
        set(messageAttachment, "createdAt", updatedAt);
        set(ticket, "createdAt", createdAt);
        set(ticket, "updatedAt", updatedAt);
        ticket.setAssignedAgent(agent);
        ticket.addAttachment(attachment);
        message.addAttachment(messageAttachment);
        ticket.addMessage(message);

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
        assertThat(response.messages()).singleElement()
                .satisfies(ticketMessageResponse -> {
                    assertThat(ticketMessageResponse.id()).isEqualTo(messageId);
                    assertThat(ticketMessageResponse.authorId()).isEqualTo(agentId);
                    assertThat(ticketMessageResponse.authorName()).isEqualTo("Agent");
                    assertThat(ticketMessageResponse.attachments()).singleElement()
                            .extracting(TicketMessageAttachmentResponse::id, TicketMessageAttachmentResponse::filename)
                            .containsExactly(messageAttachmentId, "analysis.pdf");
                });
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
        assertThat(response.messages()).isEmpty();
    }

    private void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
