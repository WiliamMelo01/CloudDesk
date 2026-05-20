package wiliammelo.clouddesk.ticket;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import wiliammelo.clouddesk.security.JwtPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Customer ticket management")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create ticket", description = "Creates a new ticket for the authenticated customer with optional image or PDF attachments.")
    @ApiResponse(responseCode = "201", description = "Ticket created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Customer or company not found")
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "multipart/form-data",
                    schema = @Schema(implementation = TicketCreateMultipartRequestDoc.class)
            )
    )
    public TicketResponse create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestPart("request") TicketCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return ticketService.create(principal.userId(), request, files);
    }

    @GetMapping("/me")
    @Operation(summary = "List current customer tickets", description = "Returns the authenticated customer's tickets.")
    @ApiResponse(responseCode = "200", description = "Tickets returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    public List<TicketResponse> listCurrentCustomerTickets(@AuthenticationPrincipal JwtPrincipal principal) {
        return ticketService.list(principal.userId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket", description = "Returns one ticket owned by the authenticated customer.")
    @ApiResponse(responseCode = "200", description = "Ticket returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Ticket not found")
    public TicketResponse get(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return ticketService.get(principal.userId(), id);
    }

    @PostMapping("/{id}/messages")
    @Operation(
            summary = "Reply to ticket",
            description = """
                    Adds a reply to one ticket owned by the authenticated customer with optional image or PDF attachments.

                    Example curl:
                    curl -X POST 'http://localhost:8080/api/tickets/{id}/messages' \\
                      -H 'Authorization: Bearer <access-token>' \\
                      -F 'request={"message":"Segue comprovante e mais detalhes."};type=application/json' \\
                      -F 'files=@/path/to/evidence.pdf;type=application/pdf' \\
                      -F 'files=@/path/to/screenshot.png;type=image/png'
                    """
    )
    @ApiResponse(responseCode = "200", description = "Reply added")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Ticket not found")
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "multipart/form-data",
                    schema = @Schema(implementation = TicketMessageMultipartRequestDoc.class)
            )
    )
    public TicketResponse reply(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestPart("request") TicketMessageRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return ticketService.replyAsCustomer(principal.userId(), id, request, files);
    }
}
