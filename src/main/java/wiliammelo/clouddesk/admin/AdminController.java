package wiliammelo.clouddesk.admin;

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
@RequestMapping("/api/admins")
@Tag(name = "Admins", description = "Admin user management")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create admin", description = "Creates a new active admin user.")
    @ApiResponse(responseCode = "201", description = "Admin created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public AdminResponse create(@Valid @RequestBody AdminCreateRequest request) {
        return adminService.create(request);
    }

    @GetMapping
    @Operation(summary = "List admins", description = "Lists active admin users.")
    @ApiResponse(responseCode = "200", description = "Admins returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    public List<AdminResponse> list() {
        return adminService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get admin", description = "Returns one active admin by id.")
    @ApiResponse(responseCode = "200", description = "Admin returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Admin not found")
    public AdminResponse get(@PathVariable UUID id) {
        return adminService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update admin", description = "Updates admin name, email, and optionally password.")
    @ApiResponse(responseCode = "200", description = "Admin updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Admin not found")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    public AdminResponse update(@PathVariable UUID id, @Valid @RequestBody AdminUpdateRequest request) {
        return adminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete admin", description = "Soft-deletes an admin by marking it inactive.")
    @ApiResponse(responseCode = "204", description = "Admin deleted")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "404", description = "Admin not found")
    public void delete(@PathVariable UUID id) {
        adminService.delete(id);
    }
}
