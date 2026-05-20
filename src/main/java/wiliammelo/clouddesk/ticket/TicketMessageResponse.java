package wiliammelo.clouddesk.ticket;

import wiliammelo.clouddesk.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record TicketMessageResponse(
        UUID id,
        UUID authorId,
        String authorName,
        UserRole authorRole,
        String message,
        java.util.List<TicketMessageAttachmentResponse> attachments,
        Instant createdAt
) {

    public static TicketMessageResponse from(TicketMessage ticketMessage) {
        return new TicketMessageResponse(
                ticketMessage.getId(),
                ticketMessage.getAuthor().getId(),
                ticketMessage.getAuthor().getName(),
                ticketMessage.getAuthorRole(),
                ticketMessage.getBody(),
                ticketMessage.getAttachments().stream()
                        .map(TicketMessageAttachmentResponse::from)
                        .toList(),
                ticketMessage.getCreatedAt()
        );
    }
}
