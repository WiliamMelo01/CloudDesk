package wiliammelo.clouddesk.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyAgentInvitationRepository extends JpaRepository<CompanyAgentInvitation, UUID> {

    boolean existsByCompanyIdAndAgentIdAndStatus(UUID companyId, UUID agentId, CompanyAgentInvitationStatus status);

    Optional<CompanyAgentInvitation> findByIdAndAgentId(UUID id, UUID agentId);

    List<CompanyAgentInvitation> findAllByAgentIdAndStatusOrderByCreatedAtDesc(
            UUID agentId,
            CompanyAgentInvitationStatus status
    );
}
