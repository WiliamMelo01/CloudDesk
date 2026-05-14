package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyResponseTest {

    @Test
    void mapsCompanyToResponse() throws Exception {
        Company company = new Company("ByteCare", "bytecare");
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-14T18:00:00Z");
        Instant updatedAt = Instant.parse("2026-05-14T19:00:00Z");
        set(company, "id", id);
        set(company, "createdAt", createdAt);
        set(company, "updatedAt", updatedAt);
        company.setLogoUrl("http://localhost:4566/clouddesk-company-assets/companies/logo.png");

        CompanyResponse response = CompanyResponse.from(company);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("ByteCare");
        assertThat(response.portalSlug()).isEqualTo("bytecare");
        assertThat(response.portalPath()).isEqualTo("/portal/bytecare");
        assertThat(response.logoUrl()).isEqualTo("http://localhost:4566/clouddesk-company-assets/companies/logo.png");
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    private void set(Company company, String fieldName, Object value) throws Exception {
        Field field = Company.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(company, value);
    }
}
