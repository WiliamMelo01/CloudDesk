package wiliammelo.clouddesk.owner;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerResponseTest {

    @Test
    void mapsFromUser() {
        User user = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        Company company = new Company("Acme", "acme", user);

        OwnerResponse response = OwnerResponse.from(user, List.of(OwnerCompanyResponse.from(company)));

        assertThat(response.id()).isNull();
        assertThat(response.name()).isEqualTo("Owner");
        assertThat(response.email()).isEqualTo("owner@cloud.test");
        assertThat(response.role()).isEqualTo(UserRole.OWNER);
        assertThat(response.active()).isTrue();
        assertThat(response.companies()).singleElement().satisfies(ownerCompany -> {
            assertThat(ownerCompany.name()).isEqualTo("Acme");
            assertThat(ownerCompany.portalSlug()).isEqualTo("acme");
            assertThat(ownerCompany.portalPath()).isEqualTo("/portal/acme");
        });
        assertThat(response.createdAt()).isNull();
        assertThat(response.updatedAt()).isNull();
    }
}
