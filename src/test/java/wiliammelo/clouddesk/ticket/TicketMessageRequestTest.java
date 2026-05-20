package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMessageRequestTest {

    @Test
    void storesMessage() {
        TicketMessageRequest request = new TicketMessageRequest("Mensagem de retorno.");

        assertThat(request.message()).isEqualTo("Mensagem de retorno.");
    }
}
