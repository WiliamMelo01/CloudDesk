package wiliammelo.clouddesk.owner;

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
@RequestMapping("/api/owners")
@Tag(name = "Owners", description = "Owner user management")
@SecurityRequirement(name = "bearerAuth")
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
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public OwnerResponse create(@Valid @RequestBody OwnerCreateRequest request) {
        return ownerService.create(request);
    }

    @GetMapping
    @Operation(summary = "List owners", description = "Lists active owner users.")
    @ApiResponse(responseCode = "200", description = "Owners returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    public List<OwnerResponse> list() {
        return ownerService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get owner", description = "Returns one active owner by id.")
    @ApiResponse(responseCode = "200", description = "Owner returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Owner not found")
    public OwnerResponse get(@PathVariable UUID id) {
        return ownerService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update owner", description = "Updates owner name, email, and optionally password.")
    @ApiResponse(responseCode = "200", description = "Owner updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Owner not found")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public OwnerResponse update(@PathVariable UUID id, @Valid @RequestBody OwnerUpdateRequest request) {
        return ownerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete owner", description = "Soft-deletes an owner by marking it inactive.")
    @ApiResponse(responseCode = "204", description = "Owner deleted")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Owner not found")
    public void delete(@PathVariable UUID id) {
        ownerService.delete(id);
    }
}
