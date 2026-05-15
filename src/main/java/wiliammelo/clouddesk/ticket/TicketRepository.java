package wiliammelo.clouddesk.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<Ticket> findAllByCompanyIdAndCompanyOwnerIdAndCompanyActiveTrueOrderByCreatedAtDesc(UUID companyId, UUID ownerId);

    List<Ticket> findAllByCompanyIdAndCompanyAgentsIdAndCompanyActiveTrueOrderByCreatedAtDesc(UUID companyId, UUID agentId);

    Optional<Ticket> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Ticket> findByIdAndCompanyIdAndCompanyOwnerIdAndCompanyActiveTrue(UUID id, UUID companyId, UUID ownerId);

    Optional<Ticket> findByIdAndCompanyIdAndCompanyAgentsIdAndCompanyActiveTrue(UUID id, UUID companyId, UUID agentId);
}
