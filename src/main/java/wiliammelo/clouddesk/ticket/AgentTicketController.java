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
@RequestMapping("/api/agents/me/companies/{companyId}/tickets")
@Tag(name = "Agent Tickets", description = "Ticket listing for company agents")
@SecurityRequirement(name = "bearerAuth")
public class AgentTicketController {

    private final TicketService ticketService;

    public AgentTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @Operation(summary = "List agent company tickets", description = "Returns all tickets from one company the authenticated agent belongs to.")
    @ApiResponse(responseCode = "200", description = "Tickets returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    public List<TicketResponse> list(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID companyId
    ) {
        return ticketService.listForAgent(principal.userId(), companyId);
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get agent company ticket", description = "Returns one ticket from a company the authenticated agent belongs to.")
    @ApiResponse(responseCode = "200", description = "Ticket returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Ticket not found")
    public TicketResponse get(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID companyId,
            @PathVariable UUID ticketId
    ) {
        return ticketService.getForAgent(principal.userId(), companyId, ticketId);
    }
}
