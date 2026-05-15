package wiliammelo.clouddesk.customer;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

class CustomerServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final CustomerService customerService = new CustomerService(userRepository, passwordEncoder);

    @Test
    void createsCustomerWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("customer@cloud.test")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.create(new CustomerCreateRequest(
                " Customer ",
                " Customer@Cloud.Test ",
                "password123"
        ));

        assertThat(response.name()).isEqualTo("Customer");
        assertThat(response.email()).isEqualTo("customer@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(response.active()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void rejectsCreateWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase("customer@cloud.test")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(new CustomerCreateRequest(
                "Customer",
                "customer@cloud.test",
                "password123"
        ))).isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void listsActiveCustomers() {
        User customer = customer("Customer", "customer@cloud.test", "hash");
        when(userRepository.findAllByRoleAndActiveTrueOrderByCreatedAtDesc(UserRole.CUSTOMER))
                .thenReturn(List.of(customer));

        List<CustomerResponse> customers = customerService.list();

        assertThat(customers).hasSize(1);
        assertThat(customers.getFirst().email()).isEqualTo("customer@cloud.test");
    }

    @Test
    void getsCustomerById() {
        UUID id = UUID.randomUUID();
        User customer = customer("Customer", "customer@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.CUSTOMER)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.get(id);

        assertThat(response.name()).isEqualTo("Customer");
    }

    @Test
    void rejectsGetWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndRole(id, UserRole.CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found.");
    }

    @Test
    void rejectsGetWhenCustomerIsInactive() {
        UUID id = UUID.randomUUID();
        User customer = customer("Customer", "customer@cloud.test", "hash");
        customer.deactivate();
        when(userRepository.findByIdAndRole(id, UserRole.CUSTOMER)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found.");
    }

    @Test
    void updatesCustomerWithoutChangingPasswordWhenPasswordIsNull() {
        UUID id = UUID.randomUUID();
        User customer = customer("Customer", "customer@cloud.test", passwordEncoder.encode("oldPassword123"));
        String originalPasswordHash = customer.getPasswordHash();
        when(userRepository.findByIdAndRole(id, UserRole.CUSTOMER)).thenReturn(Optional.of(customer));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);

        CustomerResponse response = customerService.update(id, new CustomerUpdateRequest(
                " Updated ",
                " Updated@Cloud.Test ",
                null
        ));

        assertThat(response.name()).isEqualTo("Updated");
        assertThat(response.email()).isEqualTo("updated@cloud.test");
        assertThat(customer.getPasswordHash()).isEqualTo(originalPasswordHash);
    }

    @Test
    void updatesCustomerWithoutChangingPasswordWhenPasswordIsBlank() {
        UUID id = UUID.randomUUID();
        User customer = customer("Customer", "customer@cloud.test", passwordEncoder.encode("oldPassword123"));
        String originalPasswordHash = customer.getPasswordHash();
        when(userRepository.findByIdAndRole(id, UserRole.CUSTOMER)).thenReturn(Optional.of(customer));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);

        customerService.update(id, new CustomerUpdateRequest("Updated", "updated@cloud.test", " "));

        assertThat(customer.getPasswordHash()).isEqualTo(originalPasswordHash);
    }

    @Test
    void updatesCustomerPasswordWhenProvided() {
        UUID id = UUID.randomUUID();
        User customer = customer("Customer", "customer@cloud.test", passwordEncoder.encode("oldPassword123"));
        when(userRepository.findByIdAndRole(id, UserRole.CUSTOMER)).thenReturn(Optional.of(customer));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);

        customerService.update(id, new CustomerUpdateRequest("Updated", "updated@cloud.test", "newPassword123"));

        assertThat(passwordEncoder.matches("newPassword123", customer.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsUpdateWhenEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User customer = customer("Customer", "customer@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.CUSTOMER)).thenReturn(Optional.of(customer));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("used@cloud.test", id)).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(id, new CustomerUpdateRequest(
                "Customer",
                "used@cloud.test",
                null
        ))).isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use.");
    }

    @Test
    void deletesCustomerByDeactivatingUser() {
        UUID id = UUID.randomUUID();
        User customer = customer("Customer", "customer@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.CUSTOMER)).thenReturn(Optional.of(customer));

        customerService.delete(id);

        assertThat(customer.isActive()).isFalse();
    }

    private User customer(String name, String email, String passwordHash) {
        return new User(name, email, passwordHash, UserRole.CUSTOMER);
    }
}
