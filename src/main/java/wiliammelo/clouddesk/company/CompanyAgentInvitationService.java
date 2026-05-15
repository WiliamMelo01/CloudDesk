package wiliammelo.clouddesk.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wiliammelo.clouddesk.shared.ConflictException;
import wiliammelo.clouddesk.shared.ResourceNotFoundException;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRepository;
import wiliammelo.clouddesk.user.UserRole;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyAgentInvitationService {

    private final CompanyRepository companyRepository;
    private final CompanyAgentInvitationRepository invitationRepository;
    private final UserRepository userRepository;

    public CompanyAgentInvitationService(
            CompanyRepository companyRepository,
            CompanyAgentInvitationRepository invitationRepository,
            UserRepository userRepository
    ) {
        this.companyRepository = companyRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CompanyAgentInvitationResponse create(UUID ownerId, UUID companyId, CompanyAgentInvitationCreateRequest request) {
        Company company = findCompany(companyId, ownerId);
        User agent = findAgentByEmail(request.agentEmail());

        if (companyRepository.existsByIdAndAgentsId(company.getId(), agent.getId())) {
            throw new ConflictException("Agent already belongs to this company.");
        }
        if (invitationRepository.existsByCompanyIdAndAgentIdAndStatus(
                company.getId(),
                agent.getId(),
                CompanyAgentInvitationStatus.PENDING
        )) {
            throw new ConflictException("Agent already has a pending invitation for this company.");
        }

        User owner = company.getOwner();
        CompanyAgentInvitation invitation = new CompanyAgentInvitation(company, agent, owner);
        return CompanyAgentInvitationResponse.from(invitationRepository.save(invitation));
    }

    @Transactional(readOnly = true)
    public List<CompanyAgentInvitationResponse> listPendingForAgent(UUID agentId) {
        return invitationRepository.findAllByAgentIdAndStatusOrderByCreatedAtDesc(
                        agentId,
                        CompanyAgentInvitationStatus.PENDING
                ).stream()
                .map(CompanyAgentInvitationResponse::from)
                .toList();
    }

    @Transactional
    public CompanyAgentInvitationResponse accept(UUID agentId, UUID invitationId) {
        CompanyAgentInvitation invitation = findPendingInvitation(invitationId, agentId);

        if (companyRepository.existsByIdAndAgentsId(invitation.getCompany().getId(), agentId)) {
            throw new ConflictException("Agent already belongs to this company.");
        }

        invitation.getCompany().addAgent(invitation.getAgent());
        invitation.accept();
        return CompanyAgentInvitationResponse.from(invitation);
    }

    @Transactional
    public CompanyAgentInvitationResponse reject(UUID agentId, UUID invitationId) {
        CompanyAgentInvitation invitation = findPendingInvitation(invitationId, agentId);
        invitation.reject();
        return CompanyAgentInvitationResponse.from(invitation);
    }

    private Company findCompany(UUID companyId, UUID ownerId) {
        return companyRepository.findByIdAndOwnerId(companyId, ownerId)
                .filter(Company::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
    }

    private User findAgentByEmail(String agentEmail) {
        String normalizedEmail = agentEmail.trim().toLowerCase();
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(User::isActive)
                .filter(user -> user.getRole() == UserRole.AGENT)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found."));
    }

    private CompanyAgentInvitation findPendingInvitation(UUID invitationId, UUID agentId) {
        return invitationRepository.findByIdAndAgentId(invitationId, agentId)
                .filter(invitation -> invitation.getStatus() == CompanyAgentInvitationStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
    }
}
