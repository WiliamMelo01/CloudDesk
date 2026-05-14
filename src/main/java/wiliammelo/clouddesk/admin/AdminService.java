package wiliammelo.clouddesk.admin;

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
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminResponse create(AdminCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already in use.");
        }

        User admin = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.ADMIN
        );

        return AdminResponse.from(userRepository.save(admin));
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> list() {
        return userRepository.findAllByRoleAndActiveTrueOrderByCreatedAtDesc(UserRole.ADMIN)
                .stream()
                .map(AdminResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminResponse get(UUID id) {
        return AdminResponse.from(findAdmin(id));
    }

    @Transactional
    public AdminResponse update(UUID id, AdminUpdateRequest request) {
        User admin = findAdmin(id);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("Email already in use.");
        }

        admin.setName(request.name().trim());
        admin.setEmail(email);
        if (request.password() != null && !request.password().isBlank()) {
            admin.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return AdminResponse.from(admin);
    }

    @Transactional
    public void delete(UUID id) {
        User admin = findAdmin(id);
        admin.deactivate();
    }

    private User findAdmin(UUID id) {
        return userRepository.findByIdAndRole(id, UserRole.ADMIN)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found."));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
