package wiliammelo.clouddesk.ticket;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public record TicketCreateMultipartRequestDoc(
        @Schema(
                description = "JSON payload with the ticket data. Send this as the multipart part named 'request'.",
                implementation = TicketCreateRequest.class
        )
        TicketCreateRequest request,

        @ArraySchema(
                schema = @Schema(
                        type = "string",
                        format = "binary",
                        description = "Optional attachment file. Accepted types: image/* and application/pdf."
                ),
                arraySchema = @Schema(description = "Optional list of attachment files sent as the multipart part named 'files'.")
        )
        java.util.List<String> files
) {
}
