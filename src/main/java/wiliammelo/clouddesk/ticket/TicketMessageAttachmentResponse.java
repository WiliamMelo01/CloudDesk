package wiliammelo.clouddesk.ticket;

import java.time.Instant;
import java.util.UUID;

public record TicketMessageAttachmentResponse(
        UUID id,
        String filename,
        String contentType,
        long contentLength,
        String fileUrl,
        Instant createdAt
) {

    public static TicketMessageAttachmentResponse from(TicketMessageAttachment attachment) {
        return new TicketMessageAttachmentResponse(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getContentLength(),
                attachment.getFileUrl(),
                attachment.getCreatedAt()
        );
    }
}
