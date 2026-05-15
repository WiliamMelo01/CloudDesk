package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.shared.ConflictException;
import wiliammelo.clouddesk.shared.ResourceNotFoundException;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRepository;
import wiliammelo.clouddesk.user.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompanyAgentInvitationServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final CompanyAgentInvitationRepository invitationRepository = mock(CompanyAgentInvitationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CompanyAgentInvitationService service = new CompanyAgentInvitationService(
            companyRepository,
            invitationRepository,
            userRepository
    );
    private final UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID agentId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void createsInvitation() {
        Company company = company();
        User agent = agent();
        when(companyRepository.findByIdAndOwnerId(company.getId(), ownerId)).thenReturn(Optional.of(company));
        when(userRepository.findByEmailIgnoreCase("agent@cloud.test")).thenReturn(Optional.of(agent));
        when(companyRepository.existsByIdAndAgentsId(company.getId(), agentId)).thenReturn(false);
        when(invitationRepository.existsByCompanyIdAndAgentIdAndStatus(
                company.getId(),
                agentId,
                CompanyAgentInvitationStatus.PENDING
        )).thenReturn(false);
        when(invitationRepository.save(any(CompanyAgentInvitation.class))).thenAnswer(invocation -> {
            CompanyAgentInvitation invitation = invocation.getArgument(0);
            invitation.prePersist();
            return invitation;
        });

        CompanyAgentInvitationResponse response = service.create(
                ownerId,
                company.getId(),
                new CompanyAgentInvitationCreateRequest(" Agent@Cloud.Test ")
        );

        assertThat(response.companyId()).isEqualTo(company.getId());
        assertThat(response.agentEmail()).isEqualTo("agent@cloud.test");
        assertThat(response.status()).isEqualTo(CompanyAgentInvitationStatus.PENDING);
    }

    @Test
    void rejectsInvitationWhenAgentNotFound() {
        Company company = company();
        when(companyRepository.findByIdAndOwnerId(company.getId(), ownerId)).thenReturn(Optional.of(company));
        when(userRepository.findByEmailIgnoreCase("agent@cloud.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                ownerId,
                company.getId(),
                new CompanyAgentInvitationCreateRequest("agent@cloud.test")
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agent not found.");
    }

    @Test
    void rejectsInvitationWhenEmailBelongsToNonAgentUser() {
        Company company = company();
        when(companyRepository.findByIdAndOwnerId(company.getId(), ownerId)).thenReturn(Optional.of(company));
        when(userRepository.findByEmailIgnoreCase("owner@cloud.test")).thenReturn(Optional.of(owner()));

        assertThatThrownBy(() -> service.create(
                ownerId,
                company.getId(),
                new CompanyAgentInvitationCreateRequest("owner@cloud.test")
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agent not found.");
    }

    @Test
    void rejectsInvitationWhenAgentAlreadyBelongsToCompany() {
        Company company = company();
        User agent = agent();
        when(companyRepository.findByIdAndOwnerId(company.getId(), ownerId)).thenReturn(Optional.of(company));
        when(userRepository.findByEmailIgnoreCase("agent@cloud.test")).thenReturn(Optional.of(agent));
        when(companyRepository.existsByIdAndAgentsId(company.getId(), agentId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                ownerId,
                company.getId(),
                new CompanyAgentInvitationCreateRequest("agent@cloud.test")
        )).isInstanceOf(ConflictException.class)
                .hasMessage("Agent already belongs to this company.");
    }

    @Test
    void rejectsInvitationWhenPendingInvitationAlreadyExists() {
        Company company = company();
        User agent = agent();
        when(companyRepository.findByIdAndOwnerId(company.getId(), ownerId)).thenReturn(Optional.of(company));
        when(userRepository.findByEmailIgnoreCase("agent@cloud.test")).thenReturn(Optional.of(agent));
        when(companyRepository.existsByIdAndAgentsId(company.getId(), agentId)).thenReturn(false);
        when(invitationRepository.existsByCompanyIdAndAgentIdAndStatus(
                company.getId(),
                agentId,
                CompanyAgentInvitationStatus.PENDING
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                ownerId,
                company.getId(),
                new CompanyAgentInvitationCreateRequest("agent@cloud.test")
        )).isInstanceOf(ConflictException.class)
                .hasMessage("Agent already has a pending invitation for this company.");
    }

    @Test
    void listsPendingInvitationsForAgent() {
        CompanyAgentInvitation invitation = invitation();
        when(invitationRepository.findAllByAgentIdAndStatusOrderByCreatedAtDesc(agentId, CompanyAgentInvitationStatus.PENDING))
                .thenReturn(List.of(invitation));

        List<CompanyAgentInvitationResponse> response = service.listPendingForAgent(agentId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().agentEmail()).isEqualTo("agent@cloud.test");
    }

    @Test
    void acceptsInvitationAndAddsAgentToCompany() {
        CompanyAgentInvitation invitation = invitation();
        when(invitationRepository.findByIdAndAgentId(invitation.getId(), agentId)).thenReturn(Optional.of(invitation));
        when(companyRepository.existsByIdAndAgentsId(invitation.getCompany().getId(), agentId)).thenReturn(false);

        CompanyAgentInvitationResponse response = service.accept(agentId, invitation.getId());

        assertThat(response.status()).isEqualTo(CompanyAgentInvitationStatus.ACCEPTED);
        assertThat(invitation.getCompany().getAgents()).contains(invitation.getAgent());
        assertThat(invitation.getAgent().getCompanies()).contains(invitation.getCompany());
        assertThat(response.respondedAt()).isNotNull();
    }

    @Test
    void rejectsAcceptWhenAgentAlreadyBelongsToCompany() {
        CompanyAgentInvitation invitation = invitation();
        when(invitationRepository.findByIdAndAgentId(invitation.getId(), agentId)).thenReturn(Optional.of(invitation));
        when(companyRepository.existsByIdAndAgentsId(invitation.getCompany().getId(), agentId)).thenReturn(true);

        assertThatThrownBy(() -> service.accept(agentId, invitation.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Agent already belongs to this company.");
    }

    @Test
    void rejectsInvitation() {
        CompanyAgentInvitation invitation = invitation();
        when(invitationRepository.findByIdAndAgentId(invitation.getId(), agentId)).thenReturn(Optional.of(invitation));

        CompanyAgentInvitationResponse response = service.reject(agentId, invitation.getId());

        assertThat(response.status()).isEqualTo(CompanyAgentInvitationStatus.REJECTED);
        assertThat(response.respondedAt()).isNotNull();
    }

    @Test
    void rejectsAcceptWhenInvitationNotFound() {
        UUID invitationId = UUID.randomUUID();
        when(invitationRepository.findByIdAndAgentId(invitationId, agentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(agentId, invitationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invitation not found.");
    }

    @Test
    void rejectsRejectWhenInvitationIsNotPending() {
        CompanyAgentInvitation invitation = invitation();
        invitation.reject();
        when(invitationRepository.findByIdAndAgentId(invitation.getId(), agentId)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.reject(agentId, invitation.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invitation not found.");
    }

    private CompanyAgentInvitation invitation() {
        CompanyAgentInvitation invitation = new CompanyAgentInvitation(company(), agent(), owner());
        invitation.prePersist();
        setInvitationId(invitation, UUID.fromString("44444444-4444-4444-4444-444444444444"));
        return invitation;
    }

    private Company company() {
        Company company = new Company("ByteCare", "bytecare", owner());
        setCompanyId(company, UUID.fromString("33333333-3333-3333-3333-333333333333"));
        return company;
    }

    private User owner() {
        User owner = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        setUserId(owner, ownerId);
        return owner;
    }

    private User agent() {
        User agent = new User("Agent", "agent@cloud.test", "hash", UserRole.AGENT);
        setUserId(agent, agentId);
        return agent;
    }

    private void setUserId(User user, UUID id) {
        setField(User.class, user, "id", id);
    }

    private void setCompanyId(Company company, UUID id) {
        setField(Company.class, company, "id", id);
    }

    private void setInvitationId(CompanyAgentInvitation invitation, UUID id) {
        setField(CompanyAgentInvitation.class, invitation, "id", id);
    }

    private void setField(Class<?> type, Object target, String name, Object value) {
        try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
