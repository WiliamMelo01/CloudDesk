package wiliammelo.clouddesk.ticket;

import java.time.Instant;
import java.util.UUID;

public record TicketAttachmentResponse(
        UUID id,
        String filename,
        String contentType,
        long contentLength,
        String fileUrl,
        Instant createdAt
) {

    public static TicketAttachmentResponse from(TicketAttachment attachment) {
        return new TicketAttachmentResponse(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getContentLength(),
                attachment.getFileUrl(),
                attachment.getCreatedAt()
        );
    }
}
