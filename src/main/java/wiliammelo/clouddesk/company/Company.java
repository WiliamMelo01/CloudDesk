package wiliammelo.clouddesk.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import wiliammelo.clouddesk.user.User;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String portalSlug;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 260)
    private String logoObjectKey;

    @Column(length = 500)
    private String logoUrl;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "company_agents",
            joinColumns = @JoinColumn(name = "company_id"),
            inverseJoinColumns = @JoinColumn(name = "agent_id")
    )
    private Set<User> agents = new LinkedHashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Company() {
    }

    public Company(String name, String portalSlug, User owner) {
        this.name = name;
        this.portalSlug = portalSlug;
        this.owner = owner;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPortalSlug() {
        return portalSlug;
    }

    public void setPortalSlug(String portalSlug) {
        this.portalSlug = portalSlug;
    }

    public boolean isActive() {
        return active;
    }

    public String getLogoObjectKey() {
        return logoObjectKey;
    }

    public void setLogoObjectKey(String logoObjectKey) {
        this.logoObjectKey = logoObjectKey;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public User getOwner() {
        return owner;
    }

    public Set<User> getAgents() {
        return agents;
    }

    public void addAgent(User agent) {
        agents.add(agent);
        agent.getCompanies().add(this);
    }

    public void removeAgent(User agent) {
        agents.remove(agent);
        agent.getCompanies().remove(this);
    }

    public void deactivate() {
        active = false;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
