package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyTest {

    @Test
    void managesCompanyState() throws Exception {
        Company company = new Company("ByteCare", "bytecare");

        assertThat(company.getId()).isNull();
        assertThat(company.getName()).isEqualTo("ByteCare");
        assertThat(company.getPortalSlug()).isEqualTo("bytecare");
        assertThat(company.isActive()).isTrue();

        company.setName("Updated");
        company.setPortalSlug("updated");
        company.deactivate();

        assertThat(company.getName()).isEqualTo("Updated");
        assertThat(company.getPortalSlug()).isEqualTo("updated");
        assertThat(company.isActive()).isFalse();

        company.prePersist();
        Instant createdAt = company.getCreatedAt();
        Instant updatedAt = company.getUpdatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(updatedAt).isNotNull();

        Thread.sleep(1);
        company.preUpdate();

        assertThat(company.getCreatedAt()).isEqualTo(createdAt);
        assertThat(company.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
    }
}
