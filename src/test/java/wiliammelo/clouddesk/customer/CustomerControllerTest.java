package wiliammelo.clouddesk.customer;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.security.JwtPrincipal;
import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerControllerTest {

    private final CustomerService customerService = mock(CustomerService.class);
    private final CustomerController customerController = new CustomerController(customerService);
    private final JwtPrincipal principal = new JwtPrincipal(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "customer@cloud.test",
            UserRole.CUSTOMER,
            UUID.fromString("22222222-2222-2222-2222-222222222222")
    );

    @Test
    void createsCustomer() {
        CustomerCreateRequest request = new CustomerCreateRequest("Customer", "customer@cloud.test", "password123");
        CustomerResponse response = response();
        when(customerService.create(request)).thenReturn(response);

        assertThat(customerController.create(request)).isEqualTo(response);
    }

    @Test
    void getsCurrentCustomer() {
        CustomerResponse response = response();
        when(customerService.get(principal.userId())).thenReturn(response);

        assertThat(customerController.get(principal)).isEqualTo(response);
    }

    @Test
    void updatesCurrentCustomer() {
        CustomerUpdateRequest request = new CustomerUpdateRequest("Updated", "updated@cloud.test", null);
        CustomerResponse response = response();
        when(customerService.update(principal.userId(), request)).thenReturn(response);

        assertThat(customerController.update(principal, request)).isEqualTo(response);
    }

    @Test
    void deletesCurrentCustomer() {
        customerController.delete(principal);

        verify(customerService).delete(principal.userId());
    }

    private CustomerResponse response() {
        Instant now = Instant.parse("2026-05-14T18:00:00Z");
        return new CustomerResponse(UUID.randomUUID(), "Customer", "customer@cloud.test", UserRole.CUSTOMER, true, now, now);
    }
}
