package wiliammelo.clouddesk.ticket;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID companyId,
        UUID customerId,
        UUID assignedAgentId,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        List<TicketAttachmentResponse> attachments,
        Instant createdAt,
        Instant updatedAt
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getCompany().getId(),
                ticket.getCustomer().getId(),
                ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getId() : null,
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAttachments().stream()
                        .map(TicketAttachmentResponse::from)
                        .toList(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
