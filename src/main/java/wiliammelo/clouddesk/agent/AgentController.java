package wiliammelo.clouddesk.agent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agents")
@Tag(name = "Agents", description = "Agent user management")
@SecurityRequirement(name = "bearerAuth")
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
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public AgentResponse create(@Valid @RequestBody AgentCreateRequest request) {
        return agentService.create(request);
    }

    @GetMapping
    @Operation(summary = "List agents", description = "Lists active agent users.")
    @ApiResponse(responseCode = "200", description = "Agents returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    public List<AgentResponse> list() {
        return agentService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agent", description = "Returns one active agent by id.")
    @ApiResponse(responseCode = "200", description = "Agent returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Agent not found")
    public AgentResponse get(@PathVariable UUID id) {
        return agentService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update agent", description = "Updates agent name, email, and optionally password.")
    @ApiResponse(responseCode = "200", description = "Agent updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Agent not found")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public AgentResponse update(@PathVariable UUID id, @Valid @RequestBody AgentUpdateRequest request) {
        return agentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete agent", description = "Soft-deletes an agent by marking it inactive.")
    @ApiResponse(responseCode = "204", description = "Agent deleted")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Agent not found")
    public void delete(@PathVariable UUID id) {
        agentService.delete(id);
    }
}
