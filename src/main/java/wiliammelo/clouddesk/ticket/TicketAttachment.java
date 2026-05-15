package wiliammelo.clouddesk.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_attachments")
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false)
    private long contentLength;

    @Column(nullable = false, length = 500)
    private String objectKey;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketAttachment() {
    }

    public TicketAttachment(
            Ticket ticket,
            String filename,
            String contentType,
            long contentLength,
            String objectKey,
            String fileUrl
    ) {
        this.ticket = ticket;
        this.filename = filename;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.objectKey = objectKey;
        this.fileUrl = fileUrl;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
