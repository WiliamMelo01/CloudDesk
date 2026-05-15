package wiliammelo.clouddesk.agent;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.company.CompanyRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AgentService agentService = new AgentService(userRepository, companyRepository, passwordEncoder);

    @Test
    void createsAgentWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("agent@cloud.test")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.findAllByAgentsIdAndActiveTrueOrderByCreatedAtDesc(any(UUID.class))).thenReturn(List.of());

        AgentResponse response = agentService.create(new AgentCreateRequest(
                " Agent ",
                " Agent@Cloud.Test ",
                "password123"
        ));

        assertThat(response.name()).isEqualTo("Agent");
        assertThat(response.email()).isEqualTo("agent@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.AGENT);
        assertThat(response.active()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void rejectsCreateWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase("agent@cloud.test")).thenReturn(true);

        assertThatThrownBy(() -> agentService.create(new AgentCreateRequest(
                "Agent",
                "agent@cloud.test",
                "password123"
        ))).isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void listsActiveAgents() {
        User agent = agent("Agent", "agent@cloud.test", "hash");
        when(userRepository.findAllByRoleAndActiveTrueOrderByCreatedAtDesc(UserRole.AGENT))
                .thenReturn(List.of(agent));
        when(companyRepository.findAllByAgentsIdAndActiveTrueOrderByCreatedAtDesc(agent.getId())).thenReturn(List.of());

        List<AgentResponse> agents = agentService.list();

        assertThat(agents).hasSize(1);
        assertThat(agents.getFirst().email()).isEqualTo("agent@cloud.test");
    }

    @Test
    void getsAgentById() {
        UUID id = UUID.randomUUID();
        User agent = agent("Agent", "agent@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.AGENT)).thenReturn(Optional.of(agent));
        when(companyRepository.findAllByAgentsIdAndActiveTrueOrderByCreatedAtDesc(agent.getId())).thenReturn(List.of());

        AgentResponse response = agentService.get(id);

        assertThat(response.name()).isEqualTo("Agent");
    }

    @Test
    void getsAgentWithCompanies() {
        UUID id = UUID.randomUUID();
        User agent = agent("Agent", "agent@cloud.test", "hash");
        User owner = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        Company company = new Company("Acme", "acme", owner);
        when(userRepository.findByIdAndRole(id, UserRole.AGENT)).thenReturn(Optional.of(agent));
        when(companyRepository.findAllByAgentsIdAndActiveTrueOrderByCreatedAtDesc(agent.getId())).thenReturn(List.of(company));

        AgentResponse response = agentService.get(id);

        assertThat(response.companies()).singleElement().satisfies(agentCompany -> {
            assertThat(agentCompany.name()).isEqualTo("Acme");
            assertThat(agentCompany.portalSlug()).isEqualTo("acme");
        });
    }

    @Test
    void rejectsGetWhenAgentDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndRole(id, UserRole.AGENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agent not found.");
    }

    @Test
    void rejectsGetWhenAgentIsInactive() {
        UUID id = UUID.randomUUID();
        User agent = agent("Agent", "agent@cloud.test", "hash");
        agent.deactivate();
        when(userRepository.findByIdAndRole(id, UserRole.AGENT)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> agentService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agent not found.");
    }

    @Test
    void updatesAgentWithoutChangingPasswordWhenPasswordIsNull() {
        UUID id = UUID.randomUUID();
        User agent = agent("Agent", "agent@cloud.test", passwordEncoder.encode("oldPassword123"));
        String originalPasswordHash = agent.getPasswordHash();
        when(userRepository.findByIdAndRole(id, UserRole.AGENT)).thenReturn(Optional.of(agent));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);
        when(companyRepository.findAllByAgentsIdAndActiveTrueOrderByCreatedAtDesc(agent.getId())).thenReturn(List.of());

        AgentResponse response = agentService.update(id, new AgentUpdateRequest(
                " Updated ",
                " Updated@Cloud.Test ",
                null
        ));

        assertThat(response.name()).isEqualTo("Updated");
        assertThat(response.email()).isEqualTo("updated@cloud.test");
        assertThat(agent.getPasswordHash()).isEqualTo(originalPasswordHash);
    }

    @Test
    void updatesAgentWithoutChangingPasswordWhenPasswordIsBlank() {
        UUID id = UUID.randomUUID();
        User agent = agent("Agent", "agent@cloud.test", passwordEncoder.encode("oldPassword123"));
        String originalPasswordHash = agent.getPasswordHash();
        when(userRepository.findByIdAndRole(id, UserRole.AGENT)).thenReturn(Optional.of(agent));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);
        when(companyRepository.findAllByAgentsIdAndActiveTrueOrderByCreatedAtDesc(agent.getId())).thenReturn(List.of());

        agentService.update(id, new AgentUpdateRequest("Updated", "updated@cloud.test", " "));

        assertThat(agent.getPasswordHash()).isEqualTo(originalPasswordHash);
    }

    @Test
    void updatesAgentPasswordWhenProvided() {
        UUID id = UUID.randomUUID();
        User agent = agent("Agent", "agent@cloud.test", passwordEncoder.encode("oldPassword123"));
        when(userRepository.findByIdAndRole(id, UserRole.AGENT)).thenReturn(Optional.of(agent));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);
        when(companyRepository.findAllByAgentsIdAndActiveTrueOrderByCreatedAtDesc(agent.getId())).thenReturn(List.of());

        agentService.update(id, new AgentUpdateRequest("Updated", "updated@cloud.test", "newPassword123"));

        assertThat(passwordEncoder.matches("newPassword123", agent.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsUpdateWhenEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User agent = agent("Agent", "agent@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.AGENT)).thenReturn(Optional.of(agent));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("used@cloud.test", id)).thenReturn(true);

        assertThatThrownBy(() -> agentService.update(id, new AgentUpdateRequest(
                "Agent",
                "used@cloud.test",
                null
        ))).isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use.");
    }

    @Test
    void deletesAgentByDeactivatingUser() {
        UUID id = UUID.randomUUID();
        User agent = agent("Agent", "agent@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.AGENT)).thenReturn(Optional.of(agent));

        agentService.delete(id);

        assertThat(agent.isActive()).isFalse();
    }

    private User agent(String name, String email, String passwordHash) {
        return new User(name, email, passwordHash, UserRole.AGENT);
    }
}
