package wiliammelo.clouddesk.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import wiliammelo.clouddesk.shared.BadRequestException;
import wiliammelo.clouddesk.shared.ConflictException;
import wiliammelo.clouddesk.shared.ResourceNotFoundException;
import wiliammelo.clouddesk.storage.FileStorageService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final FileStorageService fileStorageService;

    public CompanyService(CompanyRepository companyRepository, FileStorageService fileStorageService) {
        this.companyRepository = companyRepository;
        this.fileStorageService = fileStorageService;
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

    @Transactional
    public CompanyResponse uploadLogo(UUID id, MultipartFile file) {
        Company company = findCompany(id);
        validateLogo(file);

        String objectKey = "companies/" + id + "/logo/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        try {
            String logoUrl = fileStorageService.upload(
                    objectKey,
                    file.getInputStream(),
                    file.getSize(),
                    Objects.requireNonNull(file.getContentType())
            );
            deleteExistingLogo(company);
            company.setLogoObjectKey(objectKey);
            company.setLogoUrl(logoUrl);
            return CompanyResponse.from(company);
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read logo file.");
        }
    }

    @Transactional
    public void deleteLogo(UUID id) {
        Company company = findCompany(id);
        deleteExistingLogo(company);
        company.setLogoObjectKey(null);
        company.setLogoUrl(null);
    }

    private Company findCompany(UUID id) {
        return companyRepository.findById(id)
                .filter(Company::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
    }

    private String normalizeSlug(String portalSlug) {
        return portalSlug.trim().toLowerCase();
    }

    private void validateLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Logo file is required.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Logo file must be an image.");
        }
    }

    private String sanitizeFilename(String filename) {
        String value = filename == null || filename.isBlank() ? "logo" : filename.trim().toLowerCase();
        return value.replaceAll("[^a-z0-9._-]", "-");
    }

    private void deleteExistingLogo(Company company) {
        if (company.getLogoObjectKey() != null && !company.getLogoObjectKey().isBlank()) {
            fileStorageService.delete(company.getLogoObjectKey());
        }
    }
}
