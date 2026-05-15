package wiliammelo.clouddesk.owner;

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

class OwnerServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final OwnerService ownerService = new OwnerService(userRepository, companyRepository, passwordEncoder);

    @Test
    void createsOwnerWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("owner@cloud.test")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(any(UUID.class))).thenReturn(List.of());

        OwnerResponse response = ownerService.create(new OwnerCreateRequest(
                " Owner ",
                " Owner@Cloud.Test ",
                "password123"
        ));

        assertThat(response.name()).isEqualTo("Owner");
        assertThat(response.email()).isEqualTo("owner@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.OWNER);
        assertThat(response.active()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void rejectsCreateWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase("owner@cloud.test")).thenReturn(true);

        assertThatThrownBy(() -> ownerService.create(new OwnerCreateRequest(
                "Owner",
                "owner@cloud.test",
                "password123"
        ))).isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void listsActiveOwners() {
        User owner = owner("Owner", "owner@cloud.test", "hash");
        when(userRepository.findAllByRoleAndActiveTrueOrderByCreatedAtDesc(UserRole.OWNER))
                .thenReturn(List.of(owner));
        when(companyRepository.findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(owner.getId())).thenReturn(List.of());

        List<OwnerResponse> owners = ownerService.list();

        assertThat(owners).hasSize(1);
        assertThat(owners.getFirst().email()).isEqualTo("owner@cloud.test");
    }

    @Test
    void getsOwnerById() {
        UUID id = UUID.randomUUID();
        User owner = owner("Owner", "owner@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.OWNER)).thenReturn(Optional.of(owner));
        when(companyRepository.findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(owner.getId())).thenReturn(List.of());

        OwnerResponse response = ownerService.get(id);

        assertThat(response.name()).isEqualTo("Owner");
    }

    @Test
    void getsOwnerWithCompanies() {
        UUID id = UUID.randomUUID();
        User owner = owner("Owner", "owner@cloud.test", "hash");
        Company company = new Company("Acme", "acme", owner);
        when(userRepository.findByIdAndRole(id, UserRole.OWNER)).thenReturn(Optional.of(owner));
        when(companyRepository.findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(owner.getId())).thenReturn(List.of(company));

        OwnerResponse response = ownerService.get(id);

        assertThat(response.companies()).singleElement().satisfies(ownerCompany -> {
            assertThat(ownerCompany.name()).isEqualTo("Acme");
            assertThat(ownerCompany.portalSlug()).isEqualTo("acme");
        });
    }

    @Test
    void rejectsGetWhenOwnerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndRole(id, UserRole.OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownerService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Owner not found.");
    }

    @Test
    void rejectsGetWhenOwnerIsInactive() {
        UUID id = UUID.randomUUID();
        User owner = owner("Owner", "owner@cloud.test", "hash");
        owner.deactivate();
        when(userRepository.findByIdAndRole(id, UserRole.OWNER)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> ownerService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Owner not found.");
    }

    @Test
    void updatesOwnerWithoutChangingPasswordWhenPasswordIsNull() {
        UUID id = UUID.randomUUID();
        User owner = owner("Owner", "owner@cloud.test", passwordEncoder.encode("oldPassword123"));
        String originalPasswordHash = owner.getPasswordHash();
        when(userRepository.findByIdAndRole(id, UserRole.OWNER)).thenReturn(Optional.of(owner));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);
        when(companyRepository.findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(owner.getId())).thenReturn(List.of());

        OwnerResponse response = ownerService.update(id, new OwnerUpdateRequest(
                " Updated ",
                " Updated@Cloud.Test ",
                null
        ));

        assertThat(response.name()).isEqualTo("Updated");
        assertThat(response.email()).isEqualTo("updated@cloud.test");
        assertThat(owner.getPasswordHash()).isEqualTo(originalPasswordHash);
    }

    @Test
    void updatesOwnerWithoutChangingPasswordWhenPasswordIsBlank() {
        UUID id = UUID.randomUUID();
        User owner = owner("Owner", "owner@cloud.test", passwordEncoder.encode("oldPassword123"));
        String originalPasswordHash = owner.getPasswordHash();
        when(userRepository.findByIdAndRole(id, UserRole.OWNER)).thenReturn(Optional.of(owner));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);
        when(companyRepository.findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(owner.getId())).thenReturn(List.of());

        ownerService.update(id, new OwnerUpdateRequest("Updated", "updated@cloud.test", " "));

        assertThat(owner.getPasswordHash()).isEqualTo(originalPasswordHash);
    }

    @Test
    void updatesOwnerPasswordWhenProvided() {
        UUID id = UUID.randomUUID();
        User owner = owner("Owner", "owner@cloud.test", passwordEncoder.encode("oldPassword123"));
        when(userRepository.findByIdAndRole(id, UserRole.OWNER)).thenReturn(Optional.of(owner));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("updated@cloud.test", id)).thenReturn(false);
        when(companyRepository.findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(owner.getId())).thenReturn(List.of());

        ownerService.update(id, new OwnerUpdateRequest("Updated", "updated@cloud.test", "newPassword123"));

        assertThat(passwordEncoder.matches("newPassword123", owner.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsUpdateWhenEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User owner = owner("Owner", "owner@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.OWNER)).thenReturn(Optional.of(owner));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("used@cloud.test", id)).thenReturn(true);

        assertThatThrownBy(() -> ownerService.update(id, new OwnerUpdateRequest(
                "Owner",
                "used@cloud.test",
                null
        ))).isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use.");
    }

    @Test
    void deletesOwnerByDeactivatingUser() {
        UUID id = UUID.randomUUID();
        User owner = owner("Owner", "owner@cloud.test", "hash");
        when(userRepository.findByIdAndRole(id, UserRole.OWNER)).thenReturn(Optional.of(owner));

        ownerService.delete(id);

        assertThat(owner.isActive()).isFalse();
    }

    private User owner(String name, String email, String passwordHash) {
        return new User(name, email, passwordHash, UserRole.OWNER);
    }
}
