package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMessageResponseTest {

    @Test
    void mapsMessageToResponse() throws Exception {
        User owner = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        User customer = new User("Customer", "customer@cloud.test", "hash", UserRole.CUSTOMER);
        User agent = new User("Agent", "agent@cloud.test", "hash", UserRole.AGENT);
        Ticket ticket = new Ticket(
                new Company("ByteCare", "bytecare", owner),
                customer,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        );
        TicketMessage message = new TicketMessage(ticket, agent, "Ja estamos analisando.");
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-14T18:00:00Z");
        set(message, "id", id);
        set(agent, "id", authorId);
        set(message, "createdAt", createdAt);

        TicketMessageResponse response = TicketMessageResponse.from(message);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.authorId()).isEqualTo(authorId);
        assertThat(response.authorName()).isEqualTo("Agent");
        assertThat(response.authorRole()).isEqualTo(UserRole.AGENT);
        assertThat(response.message()).isEqualTo("Ja estamos analisando.");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    private void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
