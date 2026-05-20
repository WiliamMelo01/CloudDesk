package wiliammelo.clouddesk.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import wiliammelo.clouddesk.company.Company;
import wiliammelo.clouddesk.company.CompanyRepository;
import wiliammelo.clouddesk.shared.BadRequestException;
import wiliammelo.clouddesk.shared.ResourceNotFoundException;
import wiliammelo.clouddesk.storage.FileStorageService;
import wiliammelo.clouddesk.user.User;
import wiliammelo.clouddesk.user.UserRepository;
import wiliammelo.clouddesk.user.UserRole;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketServiceTest {

    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final TicketService ticketService = new TicketService(ticketRepository, companyRepository, userRepository, fileStorageService);
    private final UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void createsTicketWithoutAttachmentsWhenFilesAreNull() {
        UUID companyId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", UUID.randomUUID());
            return ticket;
        });

        TicketResponse response = ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                " Printer down ",
                " Need help ",
                TicketPriority.HIGH
        ), null);

        assertThat(response.title()).isEqualTo("Printer down");
        assertThat(response.description()).isEqualTo("Need help");
        assertThat(response.priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.attachments()).isEmpty();
        verify(fileStorageService, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void createsTicketWithoutAttachmentsWhenFilesAreEmpty() {
        UUID companyId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", UUID.randomUUID());
            return ticket;
        });

        TicketResponse response = ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.MEDIUM
        ), List.of());

        assertThat(response.attachments()).isEmpty();
    }

    @Test
    void createsTicketWithAttachments() {
        UUID companyId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile("files", "Error Shot.PNG", "image/png", "img".getBytes());
        MockMultipartFile pdf = new MockMultipartFile("files", " report .pdf ", "application/pdf", "pdf".getBytes());
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", ticketId);
            return ticket;
        });
        when(fileStorageService.upload(anyString(), any(), anyLong(), anyString()))
                .thenReturn("http://localhost/file-1", "http://localhost/file-2");

        TicketResponse response = ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.URGENT
        ), List.of(image, pdf));

        assertThat(response.attachments()).hasSize(2);
        assertThat(response.attachments().get(0).filename()).isEqualTo("error-shot.png");
        assertThat(response.attachments().get(1).filename()).isEqualTo("report-.pdf");
        verify(fileStorageService, times(2)).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void createsTicketWithDefaultAttachmentFilenameWhenOriginalFilenameIsNull() {
        UUID companyId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", UUID.randomUUID());
            return ticket;
        });
        when(fileStorageService.upload(anyString(), any(), anyLong(), anyString())).thenReturn("http://localhost/file-1");

        TicketResponse response = ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), List.of(new NullOriginalFilenameMultipartFile()));

        assertThat(response.attachments()).singleElement()
                .extracting(TicketAttachmentResponse::filename)
                .isEqualTo("attachment");
    }

    @Test
    void createsTicketWithDefaultAttachmentFilenameWhenOriginalFilenameIsBlank() {
        UUID companyId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", UUID.randomUUID());
            return ticket;
        });
        when(fileStorageService.upload(anyString(), any(), anyLong(), anyString())).thenReturn("http://localhost/file-1");

        TicketResponse response = ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), List.of(new MockMultipartFile("files", " ", "image/png", "img".getBytes())));

        assertThat(response.attachments()).singleElement()
                .extracting(TicketAttachmentResponse::filename)
                .isEqualTo("attachment");
    }

    @Test
    void listsCustomerTickets() throws Exception {
        Ticket ticket = ticket();
        set(ticket, "id", UUID.randomUUID());
        set(ticket.getCompany(), "id", UUID.randomUUID());
        set(ticket.getCustomer(), "id", customerId);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(ticket));

        List<TicketResponse> response = ticketService.list(customerId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().customerId()).isEqualTo(customerId);
    }

    @Test
    void listsOwnerTickets() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", UUID.randomUUID());
        set(ticket.getCompany(), "id", companyId);
        when(userRepository.findByIdAndRole(ownerId, UserRole.OWNER)).thenReturn(Optional.of(owner()));
        when(ticketRepository.findAllByCompanyIdAndCompanyOwnerIdAndCompanyActiveTrueOrderByCreatedAtDesc(companyId, ownerId)).thenReturn(List.of(ticket));

        List<TicketResponse> response = ticketService.listForOwner(ownerId, companyId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().companyId()).isEqualTo(companyId);
    }

    @Test
    void listsAgentTickets() throws Exception {
        UUID agentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", UUID.randomUUID());
        set(ticket.getCompany(), "id", companyId);
        when(userRepository.findByIdAndRole(agentId, UserRole.AGENT)).thenReturn(Optional.of(agent()));
        when(ticketRepository.findAllByCompanyIdAndCompanyAgentsIdAndCompanyActiveTrueOrderByCreatedAtDesc(companyId, agentId)).thenReturn(List.of(ticket));

        List<TicketResponse> response = ticketService.listForAgent(agentId, companyId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().companyId()).isEqualTo(companyId);
    }

    @Test
    void getsCustomerTicket() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", UUID.randomUUID());
        set(ticket.getCustomer(), "id", customerId);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findByIdAndCustomerId(ticketId, customerId)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.get(customerId, ticketId);

        assertThat(response.id()).isEqualTo(ticketId);
    }

    @Test
    void getsOwnerTicket() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", companyId);
        when(userRepository.findByIdAndRole(ownerId, UserRole.OWNER)).thenReturn(Optional.of(owner()));
        when(ticketRepository.findByIdAndCompanyIdAndCompanyOwnerIdAndCompanyActiveTrue(ticketId, companyId, ownerId))
                .thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.getForOwner(ownerId, companyId, ticketId);

        assertThat(response.id()).isEqualTo(ticketId);
    }

    @Test
    void getsAgentTicket() throws Exception {
        UUID agentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", companyId);
        when(userRepository.findByIdAndRole(agentId, UserRole.AGENT)).thenReturn(Optional.of(agent()));
        when(ticketRepository.findByIdAndCompanyIdAndCompanyAgentsIdAndCompanyActiveTrue(ticketId, companyId, agentId))
                .thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.getForAgent(agentId, companyId, ticketId);

        assertThat(response.id()).isEqualTo(ticketId);
    }

    @Test
    void customerRepliesToTicket() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", UUID.randomUUID());
        set(ticket.getCustomer(), "id", customerId);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findByIdAndCustomerId(ticketId, customerId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.replyAsCustomer(customerId, ticketId, new TicketMessageRequest(" Cliente enviou retorno. "), null);

        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.messages()).singleElement()
                .extracting(TicketMessageResponse::message, TicketMessageResponse::authorRole)
                .containsExactly("Cliente enviou retorno.", UserRole.CUSTOMER);
    }

    @Test
    void agentRepliesToTicket() throws Exception {
        UUID agentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", companyId);
        when(userRepository.findByIdAndRole(agentId, UserRole.AGENT)).thenReturn(Optional.of(agent()));
        when(ticketRepository.findByIdAndCompanyIdAndCompanyAgentsIdAndCompanyActiveTrue(ticketId, companyId, agentId))
                .thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.replyAsAgent(agentId, companyId, ticketId, new TicketMessageRequest("Ja iniciamos o atendimento."), null);

        assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(response.messages()).singleElement()
                .extracting(TicketMessageResponse::message, TicketMessageResponse::authorRole)
                .containsExactly("Ja iniciamos o atendimento.", UserRole.AGENT);
    }

    @Test
    void ownerRepliesToTicket() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", companyId);
        when(userRepository.findByIdAndRole(ownerId, UserRole.OWNER)).thenReturn(Optional.of(owner()));
        when(ticketRepository.findByIdAndCompanyIdAndCompanyOwnerIdAndCompanyActiveTrue(ticketId, companyId, ownerId))
                .thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.replyAsOwner(ownerId, companyId, ticketId, new TicketMessageRequest("Vamos acompanhar a tratativa."), null);

        assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(response.messages()).singleElement()
                .extracting(TicketMessageResponse::message, TicketMessageResponse::authorRole)
                .containsExactly("Vamos acompanhar a tratativa.", UserRole.OWNER);
    }

    @Test
    void customerRepliesToTicketWithAttachments() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        MockMultipartFile image = new MockMultipartFile("files", "Error Shot.PNG", "image/png", "img".getBytes());
        MockMultipartFile pdf = new MockMultipartFile("files", " report .pdf ", "application/pdf", "pdf".getBytes());
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", UUID.randomUUID());
        set(ticket.getCustomer(), "id", customerId);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findByIdAndCustomerId(ticketId, customerId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(fileStorageService.upload(anyString(), any(), anyLong(), anyString()))
                .thenReturn("http://localhost/file-1", "http://localhost/file-2");

        TicketResponse response = ticketService.replyAsCustomer(
                customerId,
                ticketId,
                new TicketMessageRequest("Cliente enviou documentos."),
                List.of(image, pdf)
        );

        assertThat(response.messages()).singleElement()
                .satisfies(message -> assertThat(message.attachments())
                        .extracting(TicketMessageAttachmentResponse::filename)
                        .containsExactly("error-shot.png", "report-.pdf"));
        verify(fileStorageService, times(2)).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void customerRepliesToTicketWithEmptyAttachmentList() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", UUID.randomUUID());
        set(ticket.getCustomer(), "id", customerId);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findByIdAndCustomerId(ticketId, customerId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.replyAsCustomer(
                customerId,
                ticketId,
                new TicketMessageRequest("Cliente respondeu sem anexos."),
                List.of()
        );

        assertThat(response.messages()).singleElement()
                .satisfies(message -> assertThat(message.attachments()).isEmpty());
        verify(fileStorageService, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void rejectsCreateWhenCustomerDoesNotExist() {
        UUID companyId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found.");
    }

    @Test
    void rejectsCreateWhenCustomerIsInactive() {
        UUID companyId = UUID.randomUUID();
        User customer = customer();
        customer.deactivate();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found.");
    }

    @Test
    void rejectsCreateWhenCompanyDoesNotExist() {
        UUID companyId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found.");
    }

    @Test
    void rejectsReplyWhenMessageIsBlank() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", UUID.randomUUID());
        set(ticket.getCustomer(), "id", customerId);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findByIdAndCustomerId(ticketId, customerId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.replyAsCustomer(customerId, ticketId, new TicketMessageRequest("   "), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Message is required.");
    }

    @Test
    void rejectsReplyWhenMessageIsNull() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", UUID.randomUUID());
        set(ticket.getCustomer(), "id", customerId);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findByIdAndCustomerId(ticketId, customerId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.replyAsCustomer(customerId, ticketId, new TicketMessageRequest(null), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Message is required.");
    }

    @Test
    void rejectsReplyWhenTicketIsClosed() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        ticket.setStatus(TicketStatus.CLOSED);
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", UUID.randomUUID());
        set(ticket.getCustomer(), "id", customerId);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findByIdAndCustomerId(ticketId, customerId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.replyAsCustomer(customerId, ticketId, new TicketMessageRequest("Quero reabrir."), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Closed tickets cannot receive replies.");
    }

    @Test
    void rejectsReplyWhenAttachmentIsInvalid() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        MockMultipartFile file = new MockMultipartFile("files", "notes.txt", "text/plain", "bad".getBytes());
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", UUID.randomUUID());
        set(ticket.getCustomer(), "id", customerId);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findByIdAndCustomerId(ticketId, customerId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.replyAsCustomer(customerId, ticketId, new TicketMessageRequest("Veja anexo."), List.of(file)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Attachment file must be an image or PDF.");
    }

    @Test
    void rejectsReplyWhenAttachmentCannotBeRead() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticket();
        set(ticket, "id", ticketId);
        set(ticket.getCompany(), "id", UUID.randomUUID());
        set(ticket.getCustomer(), "id", customerId);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findByIdAndCustomerId(ticketId, customerId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.replyAsCustomer(
                customerId,
                ticketId,
                new TicketMessageRequest("Veja anexo."),
                List.of(new MultipartFileStub())
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unable to read attachment file.");
    }

    @Test
    void rejectsCreateWhenCompanyIsInactive() {
        UUID companyId = UUID.randomUUID();
        Company company = company();
        company.deactivate();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Company not found.");
    }

    @Test
    void rejectsListWhenCustomerDoesNotExist() {
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.list(customerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found.");
    }

    @Test
    void rejectsOwnerListWhenOwnerDoesNotExist() {
        UUID ownerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(ownerId, UserRole.OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.listForOwner(ownerId, companyId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Owner not found.");
    }

    @Test
    void rejectsAgentListWhenAgentDoesNotExist() {
        UUID agentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(agentId, UserRole.AGENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.listForAgent(agentId, companyId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agent not found.");
    }

    @Test
    void rejectsOwnerListWhenOwnerIsInactive() {
        UUID ownerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        User owner = owner();
        owner.deactivate();
        when(userRepository.findByIdAndRole(ownerId, UserRole.OWNER)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> ticketService.listForOwner(ownerId, companyId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Owner not found.");
    }

    @Test
    void rejectsAgentListWhenAgentIsInactive() {
        UUID agentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        User agent = agent();
        agent.deactivate();
        when(userRepository.findByIdAndRole(agentId, UserRole.AGENT)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> ticketService.listForAgent(agentId, companyId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agent not found.");
    }

    @Test
    void rejectsGetWhenCustomerIsInactive() {
        User customer = customer();
        customer.deactivate();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> ticketService.get(customerId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found.");
    }

    @Test
    void rejectsGetWhenTicketDoesNotExist() {
        UUID ticketId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(ticketRepository.findByIdAndCustomerId(ticketId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.get(customerId, ticketId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ticket not found.");
    }

    @Test
    void rejectsOwnerGetWhenTicketDoesNotExist() {
        UUID ownerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(ownerId, UserRole.OWNER)).thenReturn(Optional.of(owner()));
        when(ticketRepository.findByIdAndCompanyIdAndCompanyOwnerIdAndCompanyActiveTrue(ticketId, companyId, ownerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getForOwner(ownerId, companyId, ticketId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ticket not found.");
    }

    @Test
    void rejectsAgentGetWhenTicketDoesNotExist() {
        UUID agentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(agentId, UserRole.AGENT)).thenReturn(Optional.of(agent()));
        when(ticketRepository.findByIdAndCompanyIdAndCompanyAgentsIdAndCompanyActiveTrue(ticketId, companyId, agentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getForAgent(agentId, companyId, ticketId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ticket not found.");
    }

    @Test
    void rejectsCreateWhenAttachmentIsNull() {
        UUID companyId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", UUID.randomUUID());
            return ticket;
        });

        assertThatThrownBy(() -> ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), java.util.Arrays.asList((org.springframework.web.multipart.MultipartFile) null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Attachment file is required.");
    }

    @Test
    void rejectsCreateWhenAttachmentIsEmpty() {
        UUID companyId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("files", "empty.png", "image/png", new byte[0]);
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", UUID.randomUUID());
            return ticket;
        });

        assertThatThrownBy(() -> ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), List.of(file)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Attachment file is required.");
    }

    @Test
    void rejectsCreateWhenAttachmentIsNotImageOrPdf() {
        UUID companyId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("files", "notes.txt", "text/plain", "bad".getBytes());
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", UUID.randomUUID());
            return ticket;
        });

        assertThatThrownBy(() -> ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), List.of(file)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Attachment file must be an image or PDF.");
    }

    @Test
    void rejectsCreateWhenAttachmentHasNoContentType() {
        UUID companyId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("files", "notes", null, "bad".getBytes());
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", UUID.randomUUID());
            return ticket;
        });

        assertThatThrownBy(() -> ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), List.of(file)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Attachment file must be an image or PDF.");
    }

    @Test
    void rejectsCreateWhenAttachmentCannotBeRead() {
        UUID companyId = UUID.randomUUID();
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", UUID.randomUUID());
            return ticket;
        });

        assertThatThrownBy(() -> ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), List.of(new MultipartFileStub())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unable to read attachment file.");
    }

    @Test
    void rollsBackUploadedAttachmentsWhenUploadFails() {
        UUID companyId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        MockMultipartFile first = new MockMultipartFile("files", "one.png", "image/png", "one".getBytes());
        MockMultipartFile second = new MockMultipartFile("files", "two.png", "image/png", "two".getBytes());
        when(userRepository.findByIdAndRole(customerId, UserRole.CUSTOMER)).thenReturn(Optional.of(customer()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            set(ticket, "id", ticketId);
            return ticket;
        });
        when(fileStorageService.upload(anyString(), any(), anyLong(), anyString()))
                .thenReturn("http://localhost/file-1")
                .thenThrow(new RuntimeException("s3 down"));

        assertThatThrownBy(() -> ticketService.create(customerId, new TicketCreateRequest(
                companyId,
                "Printer down",
                "Need help",
                TicketPriority.HIGH
        ), List.of(first, second)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("s3 down");

        verify(fileStorageService).delete(anyString());
    }

    private Ticket ticket() {
        return new Ticket(company(), customer(), "Printer down", "Need help", TicketPriority.HIGH);
    }

    private Company company() {
        return new Company("ByteCare", "bytecare", owner());
    }

    private User customer() {
        return new User("Customer", "customer@cloud.test", "hash", UserRole.CUSTOMER);
    }

    private User owner() {
        return new User("Owner", "owner@cloud.test", "hash", UserRole.OWNER);
    }

    private User agent() {
        return new User("Agent", "agent@cloud.test", "hash", UserRole.AGENT);
    }

    private void set(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("NullableProblems")
    private static class MultipartFileStub extends MockMultipartFile {
        MultipartFileStub() {
            super("files", "broken.png", "image/png", "data".getBytes());
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            throw new IOException("broken");
        }
    }

    private static class NullOriginalFilenameMultipartFile extends MockMultipartFile {
        NullOriginalFilenameMultipartFile() {
            super("files", "placeholder.png", "image/png", "data".getBytes());
        }

        @Override
        public String getOriginalFilename() {
            return null;
        }
    }
}
