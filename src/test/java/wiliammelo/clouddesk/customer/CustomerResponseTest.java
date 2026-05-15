package wiliammelo.clouddesk.customer;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerResponseTest {

    @Test
    void mapsUserToResponse() {
        User customer = new User("Customer", "customer@cloud.test", "hash", UserRole.CUSTOMER);

        CustomerResponse response = CustomerResponse.from(customer);

        assertThat(response.name()).isEqualTo("Customer");
        assertThat(response.email()).isEqualTo("customer@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(response.active()).isTrue();
    }
}
