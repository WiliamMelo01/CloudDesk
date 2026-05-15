package wiliammelo.clouddesk.company;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import wiliammelo.clouddesk.security.JwtPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Company Invitations", description = "Company membership invitation management")
public class CompanyAgentInvitationController {

    private final CompanyAgentInvitationService invitationService;

    public CompanyAgentInvitationController(CompanyAgentInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/api/companies/{companyId}/agent-invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Invite agent to company", description = "Creates a pending invitation for an agent identified by email.")
    @ApiResponse(responseCode = "201", description = "Invitation created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Company or agent not found")
    @ApiResponse(responseCode = "409", description = "Agent already belongs to company or already has a pending invitation")
    public CompanyAgentInvitationResponse create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyAgentInvitationCreateRequest request
    ) {
        return invitationService.create(principal.userId(), companyId, request);
    }

    @GetMapping("/api/agents/me/company-invitations")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List pending invitations", description = "Lists pending company invitations for the authenticated agent.")
    @ApiResponse(responseCode = "200", description = "Invitations returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    public List<CompanyAgentInvitationResponse> listPending(@AuthenticationPrincipal JwtPrincipal principal) {
        return invitationService.listPendingForAgent(principal.userId());
    }

    @PostMapping("/api/agents/me/company-invitations/{invitationId}/accept")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Accept company invitation", description = "Accepts a pending invitation and adds the authenticated agent to the company.")
    @ApiResponse(responseCode = "200", description = "Invitation accepted")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Invitation not found")
    @ApiResponse(responseCode = "409", description = "Agent already belongs to company")
    public CompanyAgentInvitationResponse accept(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID invitationId
    ) {
        return invitationService.accept(principal.userId(), invitationId);
    }

    @PostMapping("/api/agents/me/company-invitations/{invitationId}/reject")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reject company invitation", description = "Rejects a pending invitation for the authenticated agent.")
    @ApiResponse(responseCode = "200", description = "Invitation rejected")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Invitation not found")
    public CompanyAgentInvitationResponse reject(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID invitationId
    ) {
        return invitationService.reject(principal.userId(), invitationId);
    }
}
