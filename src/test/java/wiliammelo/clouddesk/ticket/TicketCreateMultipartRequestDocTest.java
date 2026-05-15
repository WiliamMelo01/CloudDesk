package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketCreateMultipartRequestDocTest {

    @Test
    void createsDocumentationWrapper() {
        TicketCreateRequest request = new TicketCreateRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Erro ao abrir relatorio",
                "O PDF do portal nao carrega para o cliente.",
                TicketPriority.HIGH
        );

        TicketCreateMultipartRequestDoc doc = new TicketCreateMultipartRequestDoc(
                request,
                List.of("print.png", "evidence.pdf")
        );

        assertThat(doc.request()).isEqualTo(request);
        assertThat(doc.files()).containsExactly("print.png", "evidence.pdf");
    }
}
