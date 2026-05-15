package wiliammelo.clouddesk.company;

import org.junit.jupiter.api.Test;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyAgentInvitationTest {

    @Test
    void managesInvitationLifecycle() {
        User owner = new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
        User agent = new User("Agent", "agent@cloud.test", "hash", UserRole.AGENT);
        Company company = new Company("ByteCare", "bytecare", owner);
        CompanyAgentInvitation invitation = new CompanyAgentInvitation(company, agent, owner);

        invitation.prePersist();

        assertThat(invitation.getCompany()).isEqualTo(company);
        assertThat(invitation.getAgent()).isEqualTo(agent);
        assertThat(invitation.getInvitedByOwner()).isEqualTo(owner);
        assertThat(invitation.getStatus()).isEqualTo(CompanyAgentInvitationStatus.PENDING);
        assertThat(invitation.getCreatedAt()).isNotNull();

        invitation.accept();
        assertThat(invitation.getStatus()).isEqualTo(CompanyAgentInvitationStatus.ACCEPTED);
        assertThat(invitation.getRespondedAt()).isNotNull();

        invitation.reject();
        assertThat(invitation.getStatus()).isEqualTo(CompanyAgentInvitationStatus.REJECTED);
    }
}
