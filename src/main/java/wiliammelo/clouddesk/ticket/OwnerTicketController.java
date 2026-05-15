package wiliammelo.clouddesk.ticket;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wiliammelo.clouddesk.security.JwtPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/owners/me/companies/{companyId}/tickets")
@Tag(name = "Owner Tickets", description = "Ticket listing for company owners")
@SecurityRequirement(name = "bearerAuth")
public class OwnerTicketController {

    private final TicketService ticketService;

    public OwnerTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @Operation(summary = "List owner company tickets", description = "Returns all tickets from one company owned by the authenticated owner.")
    @ApiResponse(responseCode = "200", description = "Tickets returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    public List<TicketResponse> list(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID companyId
    ) {
        return ticketService.listForOwner(principal.userId(), companyId);
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get owner company ticket", description = "Returns one ticket from a company owned by the authenticated owner.")
    @ApiResponse(responseCode = "200", description = "Ticket returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Ticket not found")
    public TicketResponse get(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID companyId,
            @PathVariable UUID ticketId
    ) {
        return ticketService.getForOwner(principal.userId(), companyId, ticketId);
    }
}
