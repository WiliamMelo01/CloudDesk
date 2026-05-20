package wiliammelo.clouddesk.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketMessageRequest(
        @Schema(description = "Reply body.", example = "Ja identificamos o problema e estamos aplicando a correcao.")
        @NotBlank(message = "Message is required.")
        @Size(max = 4000, message = "Message must have at most 4000 characters.")
        String message
) {
}
