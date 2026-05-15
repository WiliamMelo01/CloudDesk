package wiliammelo.clouddesk.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import wiliammelo.clouddesk.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_agent_invitations")
public class CompanyAgentInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @ManyToOne(optional = false)
    @JoinColumn(name = "agent_id", nullable = false, updatable = false)
    private User agent;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invited_by_owner_id", nullable = false, updatable = false)
    private User invitedByOwner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CompanyAgentInvitationStatus status = CompanyAgentInvitationStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant respondedAt;

    protected CompanyAgentInvitation() {
    }

    public CompanyAgentInvitation(Company company, User agent, User invitedByOwner) {
        this.company = company;
        this.agent = agent;
        this.invitedByOwner = invitedByOwner;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public User getAgent() {
        return agent;
    }

    public User getInvitedByOwner() {
        return invitedByOwner;
    }

    public CompanyAgentInvitationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void accept() {
        status = CompanyAgentInvitationStatus.ACCEPTED;
        respondedAt = Instant.now();
    }

    public void reject() {
        status = CompanyAgentInvitationStatus.REJECTED;
        respondedAt = Instant.now();
    }
}
