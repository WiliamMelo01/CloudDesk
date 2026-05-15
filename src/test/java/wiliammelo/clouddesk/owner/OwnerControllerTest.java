package wiliammelo.clouddesk.owner;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.security.JwtPrincipal;

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
    private final JwtPrincipal principal = new JwtPrincipal(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "owner@cloud.test",
            UserRole.OWNER,
            UUID.fromString("22222222-2222-2222-2222-222222222222")
    );

    @Test
    void createsOwner() {
        OwnerCreateRequest request = new OwnerCreateRequest("Owner", "owner@cloud.test", "password123");
        OwnerResponse response = response();
        when(ownerService.create(request)).thenReturn(response);

        assertThat(ownerController.create(request)).isEqualTo(response);
    }

    @Test
    void getsCurrentOwner() {
        OwnerResponse response = response();
        when(ownerService.get(principal.userId())).thenReturn(response);

        assertThat(ownerController.get(principal)).isEqualTo(response);
    }

    @Test
    void updatesCurrentOwner() {
        OwnerUpdateRequest request = new OwnerUpdateRequest("Updated", "updated@cloud.test", null);
        OwnerResponse response = response();
        when(ownerService.update(principal.userId(), request)).thenReturn(response);

        assertThat(ownerController.update(principal, request)).isEqualTo(response);
    }

    @Test
    void deletesCurrentOwner() {
        ownerController.delete(principal);

        verify(ownerService).delete(principal.userId());
    }

    private OwnerResponse response() {
        Instant now = Instant.parse("2026-05-14T18:00:00Z");
        return new OwnerResponse(
                UUID.randomUUID(),
                "Owner",
                "owner@cloud.test",
                UserRole.OWNER,
                true,
                List.of(),
                now,
                now
        );
    }
}
