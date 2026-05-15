package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyAgentInvitationResponseTest {

    @Test
    void mapsInvitationToResponse() throws Exception {
        User owner = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        User agent = new User("Agent", "agent@cloud.test", "hash", UserRole.AGENT);
        Company company = new Company("ByteCare", "bytecare", owner);
        CompanyAgentInvitation invitation = new CompanyAgentInvitation(company, agent, owner);
        set(User.class, owner, "id", UUID.fromString("11111111-1111-1111-1111-111111111111"));
        set(User.class, agent, "id", UUID.fromString("22222222-2222-2222-2222-222222222222"));
        set(Company.class, company, "id", UUID.fromString("33333333-3333-3333-3333-333333333333"));
        set(CompanyAgentInvitation.class, invitation, "id", UUID.fromString("44444444-4444-4444-4444-444444444444"));
        set(CompanyAgentInvitation.class, invitation, "createdAt", Instant.parse("2026-05-14T18:00:00Z"));

        CompanyAgentInvitationResponse response = CompanyAgentInvitationResponse.from(invitation);

        assertThat(response.companyName()).isEqualTo("ByteCare");
        assertThat(response.companyPortalSlug()).isEqualTo("bytecare");
        assertThat(response.agentEmail()).isEqualTo("agent@cloud.test");
        assertThat(response.status()).isEqualTo(CompanyAgentInvitationStatus.PENDING);
    }

    private void set(Class<?> type, Object target, String fieldName, Object value) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
