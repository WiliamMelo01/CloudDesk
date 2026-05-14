package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.shared.ConflictException;
import wiliammelo.clouddesk.shared.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final CompanyService companyService = new CompanyService(companyRepository);

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
}
