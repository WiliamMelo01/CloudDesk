package wiliammelo.clouddesk.ticket;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.company.CompanyRepository;
import wiliammelo.clouddesk.shared.BadRequestException;
import wiliammelo.clouddesk.shared.ResourceNotFoundException;
import wiliammelo.clouddesk.storage.FileStorageService;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRepository;
import wiliammelo.clouddesk.user.UserRole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public TicketService(
            TicketRepository ticketRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService
    ) {
        this.ticketRepository = ticketRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public TicketResponse create(UUID customerId, TicketCreateRequest request, List<MultipartFile> files) {
        User customer = findCustomer(customerId);
        Company company = findCompany(request.companyId());

        Ticket ticket = ticketRepository.save(new Ticket(
                company,
                customer,
                request.title().trim(),
                request.description().trim(),
                request.priority()
        ));

        uploadAttachments(ticket, files);
        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> list(UUID customerId) {
        findCustomer(customerId);
        return ticketRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForOwner(UUID ownerId, UUID companyId) {
        findUserByRole(ownerId, UserRole.OWNER, "Owner not found.");
        return ticketRepository.findAllByCompanyIdAndCompanyOwnerIdAndCompanyActiveTrueOrderByCreatedAtDesc(companyId, ownerId)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForAgent(UUID agentId, UUID companyId) {
        findUserByRole(agentId, UserRole.AGENT, "Agent not found.");
        return ticketRepository.findAllByCompanyIdAndCompanyAgentsIdAndCompanyActiveTrueOrderByCreatedAtDesc(companyId, agentId)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse get(UUID customerId, UUID id) {
        findCustomer(customerId);
        return TicketResponse.from(ticketRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found.")));
    }

    @Transactional(readOnly = true)
    public TicketResponse getForOwner(UUID ownerId, UUID companyId, UUID ticketId) {
        findUserByRole(ownerId, UserRole.OWNER, "Owner not found.");
        return TicketResponse.from(ticketRepository.findByIdAndCompanyIdAndCompanyOwnerIdAndCompanyActiveTrue(ticketId, companyId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found.")));
    }

    @Transactional(readOnly = true)
    public TicketResponse getForAgent(UUID agentId, UUID companyId, UUID ticketId) {
        findUserByRole(agentId, UserRole.AGENT, "Agent not found.");
        return TicketResponse.from(ticketRepository.findByIdAndCompanyIdAndCompanyAgentsIdAndCompanyActiveTrue(ticketId, companyId, agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found.")));
    }

    @Transactional
    public TicketResponse replyAsCustomer(UUID customerId, UUID ticketId, TicketMessageRequest request, List<MultipartFile> files) {
        User customer = findCustomer(customerId);
        Ticket ticket = ticketRepository.findByIdAndCustomerId(ticketId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found."));
        return appendMessage(ticket, customer, request.message(), files, TicketStatus.OPEN);
    }

    @Transactional
    public TicketResponse replyAsOwner(UUID ownerId, UUID companyId, UUID ticketId, TicketMessageRequest request, List<MultipartFile> files) {
        User owner = findUserByRole(ownerId, UserRole.OWNER, "Owner not found.");
        Ticket ticket = ticketRepository.findByIdAndCompanyIdAndCompanyOwnerIdAndCompanyActiveTrue(ticketId, companyId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found."));
        return appendMessage(ticket, owner, request.message(), files, TicketStatus.IN_PROGRESS);
    }

    @Transactional
    public TicketResponse replyAsAgent(UUID agentId, UUID companyId, UUID ticketId, TicketMessageRequest request, List<MultipartFile> files) {
        User agent = findUserByRole(agentId, UserRole.AGENT, "Agent not found.");
        Ticket ticket = ticketRepository.findByIdAndCompanyIdAndCompanyAgentsIdAndCompanyActiveTrue(ticketId, companyId, agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found."));
        return appendMessage(ticket, agent, request.message(), files, TicketStatus.IN_PROGRESS);
    }

    private User findCustomer(UUID customerId) {
        return findUserByRole(customerId, UserRole.CUSTOMER, "Customer not found.");
    }

    private Company findCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .filter(Company::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
    }

    private User findUserByRole(UUID userId, UserRole role, String notFoundMessage) {
        return userRepository.findByIdAndRole(userId, role)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    }

    private void uploadAttachments(Ticket ticket, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        List<String> uploadedKeys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                validateAttachment(file);
                String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
                String objectKey = "tickets/" + ticket.getId() + "/attachments/" + UUID.randomUUID() + "-" + sanitizedFilename;
                String fileUrl = fileStorageService.upload(
                        objectKey,
                        file.getInputStream(),
                        file.getSize(),
                        Objects.requireNonNull(file.getContentType())
                );
                uploadedKeys.add(objectKey);
                ticket.addAttachment(new TicketAttachment(
                        ticket,
                        sanitizedFilename,
                        file.getContentType(),
                        file.getSize(),
                        objectKey,
                        fileUrl
                ));
            }
        } catch (IOException exception) {
            rollbackUploads(uploadedKeys);
            throw new BadRequestException("Unable to read attachment file.");
        } catch (RuntimeException exception) {
            rollbackUploads(uploadedKeys);
            throw exception;
        }
    }

    private void validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attachment file is required.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || "application/pdf".equals(contentType))) {
            throw new BadRequestException("Attachment file must be an image or PDF.");
        }
    }

    private String sanitizeFilename(String filename) {
        String value = filename == null || filename.isBlank() ? "attachment" : filename.trim().toLowerCase();
        return value.replaceAll("[^a-z0-9._-]", "-");
    }

    private void rollbackUploads(List<String> uploadedKeys) {
        for (String objectKey : uploadedKeys) {
            fileStorageService.delete(objectKey);
        }
    }

    private TicketResponse appendMessage(Ticket ticket, User author, String message, List<MultipartFile> files, TicketStatus nextStatus) {
        ensureTicketAcceptsReplies(ticket);
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Message is required.");
        }

        TicketMessage ticketMessage = new TicketMessage(ticket, author, message.trim());
        uploadMessageAttachments(ticket, ticketMessage, files);
        ticket.addMessage(ticketMessage);
        ticket.setStatus(nextStatus);

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private void ensureTicketAcceptsReplies(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BadRequestException("Closed tickets cannot receive replies.");
        }
    }

    private void uploadMessageAttachments(Ticket ticket, TicketMessage ticketMessage, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        List<String> uploadedKeys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                validateAttachment(file);
                String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
                String objectKey = "tickets/" + ticket.getId() + "/messages/" + UUID.randomUUID() + "/attachments/" + sanitizedFilename;
                String fileUrl = fileStorageService.upload(
                        objectKey,
                        file.getInputStream(),
                        file.getSize(),
                        Objects.requireNonNull(file.getContentType())
                );
                uploadedKeys.add(objectKey);
                ticketMessage.addAttachment(new TicketMessageAttachment(
                        ticketMessage,
                        sanitizedFilename,
                        file.getContentType(),
                        file.getSize(),
                        objectKey,
                        fileUrl
                ));
            }
        } catch (IOException exception) {
            rollbackUploads(uploadedKeys);
            throw new BadRequestException("Unable to read attachment file.");
        } catch (RuntimeException exception) {
            rollbackUploads(uploadedKeys);
            throw exception;
        }
    }
}
