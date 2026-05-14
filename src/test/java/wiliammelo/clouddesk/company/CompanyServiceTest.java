package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import wiliammelo.clouddesk.shared.ConflictException;
import wiliammelo.clouddesk.shared.ResourceNotFoundException;
import wiliammelo.clouddesk.shared.BadRequestException;
import wiliammelo.clouddesk.storage.FileStorageService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final CompanyService companyService = new CompanyService(companyRepository, fileStorageService);

    @Test
    void createsCompanyWithNormalizedPortalSlug() {
        when(companyRepository.existsByPortalSlugIgnoreCase("bytecare")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponse response = companyService.create(new CompanyCreateRequest(
                " ByteCare ",
                " ByteCare "
        ));

        assertThat(response.name()).isEqualTo("ByteCare");
        assertThat(response.portalSlug()).isEqualTo("bytecare");
        assertThat(response.portalPath()).isEqualTo("/portal/bytecare");
        assertThat(response.active()).isTrue();
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void rejectsCreateWhenPortalSlugAlreadyExists() {
        when(companyRepository.existsByPortalSlugIgnoreCase("bytecare")).thenReturn(true);

        assertThatThrownBy(() -> companyService.create(new CompanyCreateRequest("ByteCare", "bytecare")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Portal slug already in use.");

        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void listsActiveCompanies() {
        Company company = new Company("ByteCare", "bytecare");
        when(companyRepository.findAllByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(company));

        List<CompanyResponse> companies = companyService.list();

        assertThat(companies).hasSize(1);
        assertThat(companies.getFirst().portalSlug()).isEqualTo("bytecare");
    }

    @Test
    void getsCompanyById() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));

        CompanyResponse response = companyService.get(id);

        assertThat(response.name()).isEqualTo("ByteCare");
    }

    @Test
    void rejectsGetWhenCompanyDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found.");
    }

    @Test
    void rejectsGetWhenCompanyIsInactive() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        company.deactivate();
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> companyService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found.");
    }

    @Test
    void updatesCompany() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(companyRepository.existsByPortalSlugIgnoreCaseAndIdNot("updated", id)).thenReturn(false);

        CompanyResponse response = companyService.update(id, new CompanyUpdateRequest(
                " Updated ",
                " Updated "
        ));

        assertThat(response.name()).isEqualTo("Updated");
        assertThat(response.portalSlug()).isEqualTo("updated");
        assertThat(company.getName()).isEqualTo("Updated");
        assertThat(company.getPortalSlug()).isEqualTo("updated");
    }

    @Test
    void rejectsUpdateWhenPortalSlugAlreadyExists() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(companyRepository.existsByPortalSlugIgnoreCaseAndIdNot("used", id)).thenReturn(true);

        assertThatThrownBy(() -> companyService.update(id, new CompanyUpdateRequest("ByteCare", "used")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Portal slug already in use.");
    }

    @Test
    void deletesCompanyByDeactivatingIt() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));

        companyService.delete(id);

        assertThat(company.isActive()).isFalse();
    }

    @Test
    void uploadsCompanyLogo() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        MockMultipartFile file = new MockMultipartFile("file", "Logo Image.png", "image/png", "logo".getBytes());
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(fileStorageService.upload(anyString(), any(), anyLong(), anyString()))
                .thenReturn("http://localhost:4566/clouddesk-company-assets/companies/logo.png");

        CompanyResponse response = companyService.uploadLogo(id, file);

        assertThat(response.logoUrl()).isEqualTo("http://localhost:4566/clouddesk-company-assets/companies/logo.png");
        assertThat(company.getLogoObjectKey()).startsWith("companies/" + id + "/logo/");
        assertThat(company.getLogoObjectKey()).endsWith("-logo-image.png");
        assertThat(company.getLogoUrl()).isEqualTo("http://localhost:4566/clouddesk-company-assets/companies/logo.png");
    }

    @Test
    void uploadsCompanyLogoWithDefaultFilenameWhenOriginalFilenameIsNull() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        MockMultipartFile file = new NullOriginalFilenameMultipartFile();
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(fileStorageService.upload(anyString(), any(), anyLong(), anyString()))
                .thenReturn("http://localhost/logo.png");

        companyService.uploadLogo(id, file);

        assertThat(company.getLogoObjectKey()).endsWith("-logo");
    }

    @Test
    void uploadsCompanyLogoWithDefaultFilenameWhenOriginalFilenameIsBlank() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        MockMultipartFile file = new MockMultipartFile("file", " ", "image/png", "logo".getBytes());
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(fileStorageService.upload(anyString(), any(), anyLong(), anyString()))
                .thenReturn("http://localhost/logo.png");

        companyService.uploadLogo(id, file);

        assertThat(company.getLogoObjectKey()).endsWith("-logo");
    }

    @Test
    void uploadsCompanyLogoAndDeletesPreviousLogo() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        company.setLogoObjectKey("companies/id/logo/old.png");
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", "logo".getBytes());
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(fileStorageService.upload(anyString(), any(), anyLong(), anyString()))
                .thenReturn("http://localhost/logo.png");

        companyService.uploadLogo(id, file);

        verify(fileStorageService).delete("companies/id/logo/old.png");
    }

    @Test
    void uploadsCompanyLogoWithoutDeletingBlankPreviousLogoKey() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        company.setLogoObjectKey(" ");
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", "logo".getBytes());
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(fileStorageService.upload(anyString(), any(), anyLong(), anyString()))
                .thenReturn("http://localhost/logo.png");

        companyService.uploadLogo(id, file);

        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    void rejectsMissingLogo() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.of(new Company("ByteCare", "bytecare")));

        assertThatThrownBy(() -> companyService.uploadLogo(id, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Logo file is required.");
    }

    @Test
    void rejectsEmptyLogo() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);
        when(companyRepository.findById(id)).thenReturn(Optional.of(new Company("ByteCare", "bytecare")));

        assertThatThrownBy(() -> companyService.uploadLogo(id, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Logo file is required.");
    }

    @Test
    void rejectsNonImageLogo() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "logo.txt", "text/plain", "logo".getBytes());
        when(companyRepository.findById(id)).thenReturn(Optional.of(new Company("ByteCare", "bytecare")));

        assertThatThrownBy(() -> companyService.uploadLogo(id, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Logo file must be an image.");
    }

    @Test
    void rejectsLogoWithoutContentType() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "logo", null, "logo".getBytes());
        when(companyRepository.findById(id)).thenReturn(Optional.of(new Company("ByteCare", "bytecare")));

        assertThatThrownBy(() -> companyService.uploadLogo(id, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Logo file must be an image.");
    }

    @Test
    void rejectsLogoWhenFileCannotBeRead() throws IOException {
        UUID id = UUID.randomUUID();
        MultipartFileStub file = new MultipartFileStub();
        when(companyRepository.findById(id)).thenReturn(Optional.of(new Company("ByteCare", "bytecare")));

        assertThatThrownBy(() -> companyService.uploadLogo(id, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unable to read logo file.");
    }

    @Test
    void deletesCompanyLogo() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        company.setLogoObjectKey("companies/id/logo/logo.png");
        company.setLogoUrl("http://localhost/logo.png");
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));

        companyService.deleteLogo(id);

        verify(fileStorageService).delete("companies/id/logo/logo.png");
        assertThat(company.getLogoObjectKey()).isNull();
        assertThat(company.getLogoUrl()).isNull();
    }

    @Test
    void deletesCompanyLogoWhenNoLogoExists() {
        UUID id = UUID.randomUUID();
        Company company = new Company("ByteCare", "bytecare");
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));

        companyService.deleteLogo(id);

        verify(fileStorageService, never()).delete(anyString());
    }

    @SuppressWarnings("NullableProblems")
    private static class MultipartFileStub extends MockMultipartFile {
        MultipartFileStub() {
            super("file", "logo.png", "image/png", "logo".getBytes());
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            throw new IOException("broken");
        }
    }

    private static class NullOriginalFilenameMultipartFile extends MockMultipartFile {
        NullOriginalFilenameMultipartFile() {
            super("file", "logo.png", "image/png", "logo".getBytes());
        }

        @Override
        public String getOriginalFilename() {
            return null;
        }
    }
}
