package wiliammelo.clouddesk.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TicketCreateRequest(
        @Schema(description = "Company identifier that will own the ticket.", example = "11111111-1111-1111-1111-111111111111")
        @NotNull(message = "Company id is required.")
        UUID companyId,

        @Schema(description = "Short ticket title.", example = "Erro ao abrir relatorio")
        @NotBlank(message = "Title is required.")
        @Size(max = 160, message = "Title must have at most 160 characters.")
        String title,

        @Schema(description = "Detailed description of the issue.", example = "O PDF do portal nao carrega para o cliente.")
        @NotBlank(message = "Description is required.")
        @Size(max = 4000, message = "Description must have at most 4000 characters.")
        String description,

        @Schema(description = "Ticket priority.", example = "HIGH")
        @NotNull(message = "Priority is required.")
        TicketPriority priority
) {
}
