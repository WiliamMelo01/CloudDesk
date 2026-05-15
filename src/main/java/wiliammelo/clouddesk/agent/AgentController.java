package wiliammelo.clouddesk.agent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import wiliammelo.clouddesk.security.JwtPrincipal;

@RestController
@RequestMapping("/api/agents")
@Tag(name = "Agents", description = "Agent user management")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create agent", description = "Creates a new active agent user.")
    @ApiResponse(responseCode = "201", description = "Agent created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public AgentResponse create(@Valid @RequestBody AgentCreateRequest request) {
        return agentService.create(request);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current agent", description = "Returns the authenticated agent.")
    @ApiResponse(responseCode = "200", description = "Agent returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    public AgentResponse get(@AuthenticationPrincipal JwtPrincipal principal) {
        return agentService.get(principal.userId());
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update current agent", description = "Updates the authenticated agent name, email, and optionally password.")
    @ApiResponse(responseCode = "200", description = "Agent updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Agent not found")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public AgentResponse update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody AgentUpdateRequest request
    ) {
        return agentService.update(principal.userId(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete current agent", description = "Soft-deletes the authenticated agent by marking it inactive.")
    @ApiResponse(responseCode = "204", description = "Agent deleted")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Agent not found")
    public void delete(@AuthenticationPrincipal JwtPrincipal principal) {
        agentService.delete(principal.userId());
    }
}
