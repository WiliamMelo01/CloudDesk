package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyTest {

    @Test
    void managesCompanyState() throws Exception {
        wiliammelo.clouddesk.user.User owner = new wiliammelo.clouddesk.user.User(
                "Owner",
                "owner@cloud.test",
                "hash",
                wiliammelo.clouddesk.user.UserRole.OWNER
        );
        wiliammelo.clouddesk.user.User agent = new wiliammelo.clouddesk.user.User(
                "Agent",
                "agent@cloud.test",
                "hash",
                wiliammelo.clouddesk.user.UserRole.AGENT
        );
        Company company = new Company("ByteCare", "bytecare", owner);

        assertThat(company.getId()).isNull();
        assertThat(company.getName()).isEqualTo("ByteCare");
        assertThat(company.getPortalSlug()).isEqualTo("bytecare");
        assertThat(company.getOwner()).isEqualTo(owner);
        assertThat(company.isActive()).isTrue();

        company.setName("Updated");
        company.setPortalSlug("updated");
        company.setLogoObjectKey("companies/id/logo/logo.png");
        company.setLogoUrl("http://localhost/logo.png");
        company.addAgent(agent);
        company.removeAgent(agent);
        company.deactivate();

        assertThat(company.getName()).isEqualTo("Updated");
        assertThat(company.getPortalSlug()).isEqualTo("updated");
        assertThat(company.getLogoObjectKey()).isEqualTo("companies/id/logo/logo.png");
        assertThat(company.getLogoUrl()).isEqualTo("http://localhost/logo.png");
        assertThat(company.getAgents()).isEmpty();
        assertThat(agent.getCompanies()).isEmpty();
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
