package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMessageTest {

    @Test
    void createsMessage() {
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

        message.prePersist();

        assertThat(message.getId()).isNull();
        assertThat(message.getTicket()).isEqualTo(ticket);
        assertThat(message.getAuthor()).isEqualTo(agent);
        assertThat(message.getAuthorRole()).isEqualTo(UserRole.AGENT);
        assertThat(message.getBody()).isEqualTo("Ja estamos analisando.");
        assertThat(message.getCreatedAt()).isNotNull();
    }

    @Test
    void supportsProtectedConstructor() throws Exception {
        java.lang.reflect.Constructor<TicketMessage> constructor = TicketMessage.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThat(constructor.newInstance()).isNotNull();
    }
}
