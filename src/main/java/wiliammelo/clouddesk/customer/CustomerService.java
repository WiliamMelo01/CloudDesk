package wiliammelo.clouddesk.customer;

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
public class CustomerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerResponse create(CustomerCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already in use.");
        }

        User customer = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.CUSTOMER
        );

        return CustomerResponse.from(userRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list() {
        return userRepository.findAllByRoleAndActiveTrueOrderByCreatedAtDesc(UserRole.CUSTOMER)
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        return CustomerResponse.from(findCustomer(id));
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerUpdateRequest request) {
        User customer = findCustomer(id);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("Email already in use.");
        }

        customer.setName(request.name().trim());
        customer.setEmail(email);
        if (request.password() != null && !request.password().isBlank()) {
            customer.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(UUID id) {
        User customer = findCustomer(id);
        customer.deactivate();
    }

    private User findCustomer(UUID id) {
        return userRepository.findByIdAndRole(id, UserRole.CUSTOMER)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
