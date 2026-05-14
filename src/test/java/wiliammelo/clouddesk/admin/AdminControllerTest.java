package wiliammelo.clouddesk.admin;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import wiliammelo.clouddesk.user.UserRole;

class AdminControllerTest {

    private final AdminService adminService = mock(AdminService.class);
    private final AdminController adminController = new AdminController(adminService);

    @Test
    void createsAdmin() {
        AdminCreateRequest request = new AdminCreateRequest("Admin", "admin@cloud.test", "password123");
        AdminResponse response = response();
        when(adminService.create(request)).thenReturn(response);

        assertThat(adminController.create(request)).isEqualTo(response);
    }

    @Test
    void listsAdmins() {
        List<AdminResponse> response = List.of(response());
        when(adminService.list()).thenReturn(response);

        assertThat(adminController.list()).isEqualTo(response);
    }

    @Test
    void getsAdmin() {
        UUID id = UUID.randomUUID();
        AdminResponse response = response();
        when(adminService.get(id)).thenReturn(response);

        assertThat(adminController.get(id)).isEqualTo(response);
    }

    @Test
    void updatesAdmin() {
        UUID id = UUID.randomUUID();
        AdminUpdateRequest request = new AdminUpdateRequest("Updated", "updated@cloud.test", null);
        AdminResponse response = response();
        when(adminService.update(id, request)).thenReturn(response);

        assertThat(adminController.update(id, request)).isEqualTo(response);
    }

    @Test
    void deletesAdmin() {
        UUID id = UUID.randomUUID();

        adminController.delete(id);

        verify(adminService).delete(id);
    }

    private AdminResponse response() {
        Instant now = Instant.parse("2026-05-14T18:00:00Z");
        return new AdminResponse(UUID.randomUUID(), "Admin", "admin@cloud.test", UserRole.ADMIN, true, now, now);
    }
}
