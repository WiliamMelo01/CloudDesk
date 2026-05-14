package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

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

    @Test
    void createsCompany() {
        CompanyCreateRequest request = new CompanyCreateRequest("ByteCare", "bytecare");
        CompanyResponse response = response();
        when(companyService.create(request)).thenReturn(response);

        assertThat(companyController.create(request)).isEqualTo(response);
    }

    @Test
    void listsCompanies() {
        List<CompanyResponse> response = List.of(response());
        when(companyService.list()).thenReturn(response);

        assertThat(companyController.list()).isEqualTo(response);
    }

    @Test
    void getsCompany() {
        UUID id = UUID.randomUUID();
        CompanyResponse response = response();
        when(companyService.get(id)).thenReturn(response);

        assertThat(companyController.get(id)).isEqualTo(response);
    }

    @Test
    void updatesCompany() {
        UUID id = UUID.randomUUID();
        CompanyUpdateRequest request = new CompanyUpdateRequest("Updated", "updated");
        CompanyResponse response = response();
        when(companyService.update(id, request)).thenReturn(response);

        assertThat(companyController.update(id, request)).isEqualTo(response);
    }

    @Test
    void deletesCompany() {
        UUID id = UUID.randomUUID();

        companyController.delete(id);

        verify(companyService).delete(id);
    }

    @Test
    void uploadsLogo() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", "logo".getBytes());
        CompanyResponse response = response();
        when(companyService.uploadLogo(id, file)).thenReturn(response);

        assertThat(companyController.uploadLogo(id, file)).isEqualTo(response);
    }

    @Test
    void deletesLogo() {
        UUID id = UUID.randomUUID();

        companyController.deleteLogo(id);

        verify(companyService).deleteLogo(id);
    }

    private CompanyResponse response() {
        Instant now = Instant.parse("2026-05-14T18:00:00Z");
        return new CompanyResponse(UUID.randomUUID(), "ByteCare", "bytecare", "/portal/bytecare", null, true, now, now);
    }
}
