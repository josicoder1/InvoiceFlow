package com.invoiceflow.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceflow.BaseIntegrationTest;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.DiscountType;
import com.invoiceflow.domain.model.Proposal;
import com.invoiceflow.domain.model.ProposalItem;
import com.invoiceflow.domain.model.ProposalStatus;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.RegisterUserUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.ProposalRepositoryPort;
import com.invoiceflow.domain.port.out.TokenProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class ProposalControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ClientRepositoryPort clientRepositoryPort;
    @Autowired private ProposalRepositoryPort proposalRepositoryPort;
    @Autowired private RegisterUserUseCase registerUserUseCase;
    @Autowired private TokenProviderPort tokenProviderPort;
    @Autowired private ObjectMapper objectMapper;

    private User testUser;
    private Client testClient;
    private String authHeader;

    @BeforeEach
    void setUp() {
        String uniqueEmail = "propuser_" + UUID.randomUUID() + "@example.com";
        testUser = registerUserUseCase.register(new RegisterUserUseCase.Command(
                uniqueEmail, "password123", "Proposal User"));
        String token = tokenProviderPort.generateAccessToken(testUser.getId().toString(), Map.of(
                "email", testUser.getEmail(),
                "fullName", testUser.getFullName(),
                "plan", testUser.getPlan().name()));
        authHeader = "Bearer " + token;
        testClient = Client.create(testUser.getId(), "Proposal Client", "client@mail.com", "Acme", null, null);
        testClient = clientRepositoryPort.save(testClient);
    }

    private ProposalController.ProposalRequestDto createRequest() {
        return new ProposalController.ProposalRequestDto(
                testClient.getId(), "Website Redesign",
                "Overview text", "Scope text", "Timeline text", "Pricing notes",
                LocalDate.now().plusDays(30),
                List.of(new ProposalController.ProposalItemRequestDto(
                        "UI Design", BigDecimal.ONE, BigDecimal.valueOf(800), 1)),
                DiscountType.PERCENT, BigDecimal.TEN, BigDecimal.valueOf(5),
                0);
    }

    @Test
    void testCreateProposalSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/proposals")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposalNumber").value("PROP-0001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.publicToken").exists())
                .andExpect(jsonPath("$.total").value(756.00));
    }

    @Test
    void testGetProposalsPaginated() throws Exception {
        mockMvc.perform(post("/api/v1/proposals")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/proposals")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Website Redesign"));
    }

    @Test
    void testEditDraftProposalSuccess() throws Exception {
        String response = mockMvc.perform(post("/api/v1/proposals")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        int version = objectMapper.readTree(response).get("version").asInt();

        ProposalController.ProposalRequestDto edit = new ProposalController.ProposalRequestDto(
                testClient.getId(), "Updated Title",
                "New overview", "New scope", "New timeline", "New notes",
                LocalDate.now().plusDays(45),
                List.of(new ProposalController.ProposalItemRequestDto(
                        "Development", BigDecimal.ONE, BigDecimal.valueOf(1200), 1)),
                null, BigDecimal.ZERO, BigDecimal.ZERO, version);

        mockMvc.perform(put("/api/v1/proposals/" + id)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(edit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void testEditSentProposalReturns422() throws Exception {
        Proposal proposal = Proposal.create(
                testUser.getId(), testClient.getId(), "PROP-0099", "Sent Proposal",
                null, null, null, null, null,
                List.of(ProposalItem.create("Item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, BigDecimal.ZERO, BigDecimal.ZERO);
        proposal.send();
        proposal = proposalRepositoryPort.save(proposal);

        ProposalController.ProposalRequestDto edit = new ProposalController.ProposalRequestDto(
                testClient.getId(), "Cannot Edit", null, null, null, null, null,
                List.of(new ProposalController.ProposalItemRequestDto("X", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, BigDecimal.ZERO, BigDecimal.ZERO, proposal.getVersion());

        mockMvc.perform(put("/api/v1/proposals/" + proposal.getId())
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(edit)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business Rule Violation"));
    }

    @Test
    void testSendProposalSuccess() throws Exception {
        String response = mockMvc.perform(post("/api/v1/proposals")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());

        mockMvc.perform(post("/api/v1/proposals/" + id + "/send")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void testPublicViewTransitionsToViewed() throws Exception {
        Proposal proposal = Proposal.create(
                testUser.getId(), testClient.getId(), "PROP-0100", "Public Proposal",
                "Overview", "Scope", "Timeline", "Notes",
                LocalDate.now().plusDays(10),
                List.of(ProposalItem.create("Item", BigDecimal.ONE, BigDecimal.valueOf(100), 1)),
                null, BigDecimal.ZERO, BigDecimal.ZERO);
        proposal.send();
        proposal = proposalRepositoryPort.save(proposal);

        mockMvc.perform(get("/api/v1/public/proposals/" + proposal.getPublicToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VIEWED"))
                .andExpect(jsonPath("$.isActionable").value(true))
                .andExpect(jsonPath("$.from.name").exists())
                .andExpect(jsonPath("$.to.name").value("Proposal Client"));
    }

    @Test
    void testAcceptProposalSuccess() throws Exception {
        Proposal proposal = Proposal.create(
                testUser.getId(), testClient.getId(), "PROP-0101", "Accept Me",
                null, null, null, null, LocalDate.now().plusDays(10),
                List.of(ProposalItem.create("Item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, BigDecimal.ZERO, BigDecimal.ZERO);
        proposal.send();
        proposal = proposalRepositoryPort.save(proposal);

        String body = objectMapper.writeValueAsString(
                Map.of("signatureData", "data:image/png;base64,iVBORw0KGgo="));

        mockMvc.perform(post("/api/v1/public/proposals/" + proposal.getPublicToken() + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void testAcceptAlreadyAcceptedReturns422() throws Exception {
        Proposal proposal = Proposal.create(
                testUser.getId(), testClient.getId(), "PROP-0102", "Already Accepted",
                null, null, null, null, LocalDate.now().plusDays(10),
                List.of(ProposalItem.create("Item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, BigDecimal.ZERO, BigDecimal.ZERO);
        proposal.send();
        proposal.accept("data:image/png;base64,abc");
        proposal = proposalRepositoryPort.save(proposal);

        String body = objectMapper.writeValueAsString(
                Map.of("signatureData", "data:image/png;base64,iVBORw0KGgo="));

        mockMvc.perform(post("/api/v1/public/proposals/" + proposal.getPublicToken() + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testDeclineProposalSuccess() throws Exception {
        Proposal proposal = Proposal.create(
                testUser.getId(), testClient.getId(), "PROP-0103", "Decline Me",
                null, null, null, null, LocalDate.now().plusDays(10),
                List.of(ProposalItem.create("Item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, BigDecimal.ZERO, BigDecimal.ZERO);
        proposal.send();
        proposal = proposalRepositoryPort.save(proposal);

        mockMvc.perform(post("/api/v1/public/proposals/" + proposal.getPublicToken() + "/decline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    void testConvertToInvoiceWhenAccepted() throws Exception {
        Proposal proposal = Proposal.create(
                testUser.getId(), testClient.getId(), "PROP-0104", "Convert Me",
                null, null, null, "Payment notes",
                LocalDate.now().plusDays(10),
                List.of(ProposalItem.create("Item", BigDecimal.ONE, BigDecimal.valueOf(250), 1)),
                DiscountType.PERCENT, BigDecimal.valueOf(10), BigDecimal.valueOf(5));
        proposal.send();
        proposal.accept("data:image/png;base64,abc");
        proposal = proposalRepositoryPort.save(proposal);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/convert-to-invoice")
                        .header("Authorization", authHeader))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.notes").value("Payment notes"));
    }

    @Test
    void testConvertToInvoiceWhenDraftReturns422() throws Exception {
        String response = mockMvc.perform(post("/api/v1/proposals")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());

        mockMvc.perform(post("/api/v1/proposals/" + id + "/convert-to-invoice")
                        .header("Authorization", authHeader))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testDownloadProposalPdf() throws Exception {
        String response = mockMvc.perform(post("/api/v1/proposals")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());

        mockMvc.perform(get("/api/v1/proposals/" + id + "/pdf")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}
