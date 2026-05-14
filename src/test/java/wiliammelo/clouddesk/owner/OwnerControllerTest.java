package wiliammelo.clouddesk.owner;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import wiliammelo.clouddesk.user.UserRole;

class OwnerControllerTest {

    private final OwnerService ownerService = mock(OwnerService.class);
    private final OwnerController ownerController = new OwnerController(ownerService);

    @Test
    void createsOwner() {
        OwnerCreateRequest request = new OwnerCreateRequest("Owner", "owner@cloud.test", "password123");
        OwnerResponse response = response();
        when(ownerService.create(request)).thenReturn(response);

        assertThat(ownerController.create(request)).isEqualTo(response);
    }

    @Test
    void listsOwners() {
        List<OwnerResponse> response = List.of(response());
        when(ownerService.list()).thenReturn(response);

        assertThat(ownerController.list()).isEqualTo(response);
    }

    @Test
    void getsOwner() {
        UUID id = UUID.randomUUID();
        OwnerResponse response = response();
        when(ownerService.get(id)).thenReturn(response);

        assertThat(ownerController.get(id)).isEqualTo(response);
    }

    @Test
    void updatesOwner() {
        UUID id = UUID.randomUUID();
        OwnerUpdateRequest request = new OwnerUpdateRequest("Updated", "updated@cloud.test", null);
        OwnerResponse response = response();
        when(ownerService.update(id, request)).thenReturn(response);

        assertThat(ownerController.update(id, request)).isEqualTo(response);
    }

    @Test
    void deletesOwner() {
        UUID id = UUID.randomUUID();

        ownerController.delete(id);

        verify(ownerService).delete(id);
    }

    private OwnerResponse response() {
        Instant now = Instant.parse("2026-05-14T18:00:00Z");
        return new OwnerResponse(UUID.randomUUID(), "Owner", "owner@cloud.test", UserRole.OWNER, true, now, now);
    }
}
