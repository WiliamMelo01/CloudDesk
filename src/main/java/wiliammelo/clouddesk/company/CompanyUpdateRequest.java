package wiliammelo.clouddesk.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(
        @NotBlank
        @Size(max = 160)
        String name,

        @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "^[a-zA-Z0-9-]+$")
        String portalSlug
) {
}
