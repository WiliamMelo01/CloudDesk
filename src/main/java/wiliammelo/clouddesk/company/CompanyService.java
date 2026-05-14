package wiliammelo.clouddesk.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wiliammelo.clouddesk.shared.ConflictException;
import wiliammelo.clouddesk.shared.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional
    public CompanyResponse create(CompanyCreateRequest request) {
        String portalSlug = normalizeSlug(request.portalSlug());
        if (companyRepository.existsByPortalSlugIgnoreCase(portalSlug)) {
            throw new ConflictException("Portal slug already in use.");
        }

        Company company = new Company(request.name().trim(), portalSlug);
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> list() {
        return companyRepository.findAllByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(UUID id) {
        return CompanyResponse.from(findCompany(id));
    }

    @Transactional
    public CompanyResponse update(UUID id, CompanyUpdateRequest request) {
        Company company = findCompany(id);
        String portalSlug = normalizeSlug(request.portalSlug());
        if (companyRepository.existsByPortalSlugIgnoreCaseAndIdNot(portalSlug, id)) {
            throw new ConflictException("Portal slug already in use.");
        }

        company.setName(request.name().trim());
        company.setPortalSlug(portalSlug);
        return CompanyResponse.from(company);
    }

    @Transactional
    public void delete(UUID id) {
        Company company = findCompany(id);
        company.deactivate();
    }

    private Company findCompany(UUID id) {
        return companyRepository.findById(id)
                .filter(Company::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
    }

    private String normalizeSlug(String portalSlug) {
        return portalSlug.trim().toLowerCase();
    }
}
