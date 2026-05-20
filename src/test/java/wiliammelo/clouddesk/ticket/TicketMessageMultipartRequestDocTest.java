package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMessageMultipartRequestDocTest {

    @Test
    void createsDocumentationWrapper() {
        TicketMessageRequest request = new TicketMessageRequest("Ja iniciamos o atendimento.");

        TicketMessageMultipartRequestDoc doc = new TicketMessageMultipartRequestDoc(
                request,
                List.of("print.png", "evidence.pdf")
        );

        assertThat(doc.request()).isEqualTo(request);
        assertThat(doc.files()).containsExactly("print.png", "evidence.pdf");
    }
}
