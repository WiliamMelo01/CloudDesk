package wiliammelo.clouddesk.company;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import wiliammelo.clouddesk.security.JwtPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Companies", description = "Company workspace management")
@SecurityRequirement(name = "bearerAuth")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create company", description = "Creates a company workspace and its support portal slug.")
    @ApiResponse(responseCode = "201", description = "Company created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "403", description = "Authenticated user is not an owner")
    @ApiResponse(responseCode = "409", description = "Portal slug already in use")
    public CompanyResponse create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CompanyCreateRequest request
    ) {
        return companyService.create(principal.userId(), request);
    }

    @GetMapping
    @Operation(summary = "List companies", description = "Lists active company workspaces.")
    @ApiResponse(responseCode = "200", description = "Companies returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "403", description = "Authenticated user is not an owner")
    public List<CompanyResponse> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return companyService.list(principal.userId());
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get company by slug", description = "Returns one active company workspace by portal slug for public portal usage.")
    @ApiResponse(responseCode = "200", description = "Company returned")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse getBySlug(@PathVariable String slug) {
        return companyService.getBySlug(slug);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company", description = "Returns one active company workspace by id.")
    @ApiResponse(responseCode = "200", description = "Company returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "403", description = "Authenticated user is not an owner")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse get(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return companyService.get(principal.userId(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update company", description = "Updates company name and support portal slug.")
    @ApiResponse(responseCode = "200", description = "Company updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "403", description = "Authenticated user is not an owner")
    @ApiResponse(responseCode = "404", description = "Company not found")
    @ApiResponse(responseCode = "409", description = "Portal slug already in use")
    public CompanyResponse update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CompanyUpdateRequest request
    ) {
        return companyService.update(principal.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete company", description = "Soft-deletes a company by marking it inactive.")
    @ApiResponse(responseCode = "204", description = "Company deleted")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "403", description = "Authenticated user is not an owner")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public void delete(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        companyService.delete(principal.userId(), id);
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload company logo", description = "Uploads a company logo image to S3 and returns the updated company.")
    @ApiResponse(responseCode = "200", description = "Logo uploaded")
    @ApiResponse(responseCode = "400", description = "Missing or invalid logo file")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "403", description = "Authenticated user is not an owner")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public CompanyResponse uploadLogo(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file
    ) {
        return companyService.uploadLogo(principal.userId(), id, file);
    }

    @DeleteMapping("/{id}/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete company logo", description = "Deletes the company logo from S3 and clears the logo URL.")
    @ApiResponse(responseCode = "204", description = "Logo deleted")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "403", description = "Authenticated user is not an owner")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public void deleteLogo(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        companyService.deleteLogo(principal.userId(), id);
    }
}
