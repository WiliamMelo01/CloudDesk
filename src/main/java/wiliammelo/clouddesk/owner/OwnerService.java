package wiliammelo.clouddesk.owner;

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
public class OwnerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OwnerService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OwnerResponse create(OwnerCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already in use.");
        }

        User owner = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.OWNER
        );

        return OwnerResponse.from(userRepository.save(owner));
    }

    @Transactional(readOnly = true)
    public List<OwnerResponse> list() {
        return userRepository.findAllByRoleAndActiveTrueOrderByCreatedAtDesc(UserRole.OWNER)
                .stream()
                .map(OwnerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnerResponse get(UUID id) {
        return OwnerResponse.from(findOwner(id));
    }

    @Transactional
    public OwnerResponse update(UUID id, OwnerUpdateRequest request) {
        User owner = findOwner(id);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("Email already in use.");
        }

        owner.setName(request.name().trim());
        owner.setEmail(email);
        if (request.password() != null && !request.password().isBlank()) {
            owner.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return OwnerResponse.from(owner);
    }

    @Transactional
    public void delete(UUID id) {
        User owner = findOwner(id);
        owner.deactivate();
    }

    private User findOwner(UUID id) {
        return userRepository.findByIdAndRole(id, UserRole.OWNER)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found."));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
