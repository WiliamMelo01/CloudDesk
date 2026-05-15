package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import wiliammelo.clouddesk.security.JwtPrincipal;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyControllerTest {

    private final CompanyService companyService = mock(CompanyService.class);
    private final CompanyController companyController = new CompanyController(companyService);
    private final JwtPrincipal principal = new JwtPrincipal(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "owner@cloud.test",
            UserRole.OWNER,
            UUID.fromString("22222222-2222-2222-2222-222222222222")
    );

    @Test
    void createsCompany() {
        CompanyCreateRequest request = new CompanyCreateRequest("ByteCare", "bytecare");
        CompanyResponse response = response();
        when(companyService.create(principal.userId(), request)).thenReturn(response);

        assertThat(companyController.create(principal, request)).isEqualTo(response);
    }

    @Test
    void listsCompanies() {
        List<CompanyResponse> response = List.of(response());
        when(companyService.list(principal.userId())).thenReturn(response);

        assertThat(companyController.list(principal)).isEqualTo(response);
    }

    @Test
    void getsCompanyBySlug() {
        CompanyResponse response = response();
        when(companyService.getBySlug("bytecare")).thenReturn(response);

        assertThat(companyController.getBySlug("bytecare")).isEqualTo(response);
    }

    @Test
    void getsCompany() {
        UUID id = UUID.randomUUID();
        CompanyResponse response = response();
        when(companyService.get(principal.userId(), id)).thenReturn(response);

        assertThat(companyController.get(principal, id)).isEqualTo(response);
    }

    @Test
    void updatesCompany() {
        UUID id = UUID.randomUUID();
        CompanyUpdateRequest request = new CompanyUpdateRequest("Updated", "updated");
        CompanyResponse response = response();
        when(companyService.update(principal.userId(), id, request)).thenReturn(response);

        assertThat(companyController.update(principal, id, request)).isEqualTo(response);
    }

    @Test
    void deletesCompany() {
        UUID id = UUID.randomUUID();

        companyController.delete(principal, id);

        verify(companyService).delete(principal.userId(), id);
    }

    @Test
    void uploadsLogo() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", "logo".getBytes());
        CompanyResponse response = response();
        when(companyService.uploadLogo(principal.userId(), id, file)).thenReturn(response);

        assertThat(companyController.uploadLogo(principal, id, file)).isEqualTo(response);
    }

    @Test
    void deletesLogo() {
        UUID id = UUID.randomUUID();

        companyController.deleteLogo(principal, id);

        verify(companyService).deleteLogo(principal.userId(), id);
    }

    private CompanyResponse response() {
        Instant now = Instant.parse("2026-05-14T18:00:00Z");
        return new CompanyResponse(UUID.randomUUID(), "ByteCare", "bytecare", "/portal/bytecare", null, true, now, now);
    }
}
