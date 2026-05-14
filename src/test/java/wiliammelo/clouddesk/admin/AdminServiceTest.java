package wiliammelo.clouddesk.admin;

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

class AdminServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AdminService adminService = new AdminService(userRepository, passwordEncoder);

    @Test
    void createsAdminWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("admin@cloud.test")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminResponse response = adminService.create(new AdminCreateRequest(
                " Admin ",
                " Admin@Cloud.Test ",
                "password123"
        ));

        assertThat(response.name()).isEqualTo("Admin");
        assertThat(response.email()).isEqualTo("admin@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.active()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void rejectsCreateWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase("admin@cloud.test")).thenReturn(true);

        assertThatThrownBy(() -> adminService.create(new AdminCreateRequest(
                "Admin",
                "admin@cloud.test",
                "password123"
        ))).isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void listsActiveAdmins() {
        User admin = admin("Admin", "admin@cloud.test", "hash");
        when(userRepository.findAllByRoleAndActiveTrueOrderByCreatedAtDesc(UserRole.ADMIN))
                .thenReturn(List.of(admin));

        List<AdminResponse> admins = adminService.list();

        assertThat(admins).hasSize(1);
        assertThat(admins.getFirst().email()).isEqualTo("admin@cloud.test");
    }

    @Test
    void getsAdminById() {
        UUID id = UUID.randomUUID();
        User admin = admin("Admin", "admin@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.ADMIN)).thenReturn(Optional.of(admin));

        AdminResponse response = adminService.get(id);

        assertThat(response.name()).isEqualTo("Admin");
    }

    @Test
    void rejectsGetWhenAdminDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndRole(id, UserRole.ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Admin not found.");
    }

    @Test
    void rejectsGetWhenAdminIsInactive() {
        UUID id = UUID.randomUUID();
        User admin = admin("Admin", "admin@cloud.test", "hash");
        admin.deactivate();
        when(userRepository.findByIdAndRole(id, UserRole.ADMIN)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Admin not found.");
    }

    @Test
    void updatesAdminWithoutChangingPasswordWhenPasswordIsNull() {
        UUID id = UUID.randomUUID();
        User admin = admin("Admin", "admin@cloud.test", passwordEncoder.encode("oldPassword123"));
        String originalPasswordHash = admin.getPasswordHash();
        when(userRepository.findByIdAndRole(id, UserRole.ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);

        AdminResponse response = adminService.update(id, new AdminUpdateRequest(
                " Updated ",
                " Updated@Cloud.Test ",
                null
        ));

        assertThat(response.name()).isEqualTo("Updated");
        assertThat(response.email()).isEqualTo("updated@cloud.test");
        assertThat(admin.getPasswordHash()).isEqualTo(originalPasswordHash);
    }

    @Test
    void updatesAdminWithoutChangingPasswordWhenPasswordIsBlank() {
        UUID id = UUID.randomUUID();
        User admin = admin("Admin", "admin@cloud.test", passwordEncoder.encode("oldPassword123"));
        String originalPasswordHash = admin.getPasswordHash();
        when(userRepository.findByIdAndRole(id, UserRole.ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);

        adminService.update(id, new AdminUpdateRequest("Updated", "updated@cloud.test", " "));

        assertThat(admin.getPasswordHash()).isEqualTo(originalPasswordHash);
    }

    @Test
    void updatesAdminPasswordWhenProvided() {
        UUID id = UUID.randomUUID();
        User admin = admin("Admin", "admin@cloud.test", passwordEncoder.encode("oldPassword123"));
        when(userRepository.findByIdAndRole(id, UserRole.ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);

        adminService.update(id, new AdminUpdateRequest("Updated", "updated@cloud.test", "newPassword123"));

        assertThat(passwordEncoder.matches("newPassword123", admin.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsUpdateWhenEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User admin = admin("Admin", "admin@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("used@cloud.test", id)).thenReturn(true);

        assertThatThrownBy(() -> adminService.update(id, new AdminUpdateRequest(
                "Admin",
                "used@cloud.test",
                null
        ))).isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use.");
    }

    @Test
    void deletesAdminByDeactivatingUser() {
        UUID id = UUID.randomUUID();
        User admin = admin("Admin", "admin@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.ADMIN)).thenReturn(Optional.of(admin));

        adminService.delete(id);

        assertThat(admin.isActive()).isFalse();
    }

    private User admin(String name, String email, String passwordHash) {
        return new User(name, email, passwordHash, UserRole.ADMIN);
    }
}
