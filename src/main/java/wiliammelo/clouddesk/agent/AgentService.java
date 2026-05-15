package wiliammelo.clouddesk.agent;

import org.springframework.security.crypto.password.PasswordEncoder;
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
public class AgentService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AgentService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AgentResponse create(AgentCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already in use.");
        }

        User agent = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.AGENT
        );

        return AgentResponse.from(userRepository.save(agent));
    }

    @Transactional(readOnly = true)
    public List<AgentResponse> list() {
        return userRepository.findAllByRoleAndActiveTrueOrderByCreatedAtDesc(UserRole.AGENT)
                .stream()
                .map(AgentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentResponse get(UUID id) {
        return AgentResponse.from(findAgent(id));
    }

    @Transactional
    public AgentResponse update(UUID id, AgentUpdateRequest request) {
        User agent = findAgent(id);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("Email already in use.");
        }

        agent.setName(request.name().trim());
        agent.setEmail(email);
        if (request.password() != null && !request.password().isBlank()) {
            agent.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return AgentResponse.from(agent);
    }

    @Transactional
    public void delete(UUID id) {
        User agent = findAgent(id);
        agent.deactivate();
    }

    private User findAgent(UUID id) {
        return userRepository.findByIdAndRole(id, UserRole.AGENT)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found."));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
