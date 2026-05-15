package wiliammelo.clouddesk.owner;

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
@RequestMapping("/api/owners")
@Tag(name = "Owners", description = "Owner user management")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create owner", description = "Creates a new active owner user.")
    @ApiResponse(responseCode = "201", description = "Owner created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public OwnerResponse create(@Valid @RequestBody OwnerCreateRequest request) {
        return ownerService.create(request);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current owner", description = "Returns the authenticated owner.")
    @ApiResponse(responseCode = "200", description = "Owner returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    public OwnerResponse get(@AuthenticationPrincipal JwtPrincipal principal) {
        return ownerService.get(principal.userId());
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update current owner", description = "Updates owner name, email, and optionally password.")
    @ApiResponse(responseCode = "200", description = "Owner updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Owner not found")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public OwnerResponse update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody OwnerUpdateRequest request
    ) {
        return ownerService.update(principal.userId(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete current owner", description = "Soft-deletes the authenticated owner by marking it inactive.")
    @ApiResponse(responseCode = "204", description = "Owner deleted")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Owner not found")
    public void delete(@AuthenticationPrincipal JwtPrincipal principal) {
        ownerService.delete(principal.userId());
    }
}
