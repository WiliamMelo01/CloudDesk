package wiliammelo.clouddesk.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByPortalSlugIgnoreCase(String portalSlug);

    boolean existsByPortalSlugIgnoreCaseAndIdNot(String portalSlug, UUID id);

    List<Company> findAllByActiveTrueOrderByCreatedAtDesc();
}
