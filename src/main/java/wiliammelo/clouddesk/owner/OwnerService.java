package wiliammelo.clouddesk.owner;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wiliammelo.clouddesk.company.CompanyRepository;
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
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public OwnerService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
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

        return toResponse(userRepository.save(owner));
    }

    @Transactional(readOnly = true)
    public List<OwnerResponse> list() {
        return userRepository.findAllByRoleAndActiveTrueOrderByCreatedAtDesc(UserRole.OWNER)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnerResponse get(UUID id) {
        return toResponse(findOwner(id));
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

        return toResponse(owner);
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

    private OwnerResponse toResponse(User owner) {
        List<OwnerCompanyResponse> companies = companyRepository.findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(owner.getId())
                .stream()
                .map(OwnerCompanyResponse::from)
                .toList();
        return OwnerResponse.from(owner, companies);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
