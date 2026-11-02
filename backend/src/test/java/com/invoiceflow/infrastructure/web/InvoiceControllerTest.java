package com.invoiceflow.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceflow.BaseIntegrationTest;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.DiscountType;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.RegisterUserUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
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
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class InvoiceControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepositoryPort clientRepositoryPort;

    @Autowired
    private InvoiceRepositoryPort invoiceRepositoryPort;

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Client testClient;
    private String authHeader;

    @BeforeEach
    void setUp() {
        String uniqueEmail = "user_" + UUID.randomUUID() + "@example.com";
        testUser = registerUserUseCase.register(new RegisterUserUseCase.Command(
                uniqueEmail, "password123", "Integration User"
        ));

        String token = tokenProviderPort.generateAccessToken(testUser.getId().toString(), Map.of(
                "email", testUser.getEmail(),
                "fullName", testUser.getFullName(),
                "plan", testUser.getPlan().name()
        ));
        authHeader = "Bearer " + token;

        testClient = Client.create(testUser.getId(), "Test Client", "testclient@mail.com", null, null, null);
        testClient = clientRepositoryPort.save(testClient);
    }

    @Test
    void testCreateInvoiceSuccess() throws Exception {
        InvoiceController.InvoiceItemRequestDto itemDto = new InvoiceController.InvoiceItemRequestDto(
                "Consulting Services", BigDecimal.valueOf(5), BigDecimal.valueOf(100), 1
        );
        InvoiceController.InvoiceRequestDto request = new InvoiceController.InvoiceRequestDto(
                testClient.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "USD",
                Collections.singletonList(itemDto),
                DiscountType.PERCENT,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(5),
                "Test notes",
                "Net 30"
        );

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-0001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.subtotal").value(500.00))
                .andExpect(jsonPath("$.total").value(472.50)) // 500 - 50 discount = 450 + 22.50 tax = 472.50
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].description").value("Consulting Services"));
    }

    @Test
    void testDuplicateInvoiceSuccess() throws Exception {
        Invoice invoice = Invoice.create(
                testUser.getId(), testClient.getId(), "INV-0001",
                LocalDate.now(), LocalDate.now().plusDays(30), "USD",
                Collections.singletonList(com.invoiceflow.domain.model.InvoiceItem.create("item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, null, null, null, null
        );
        invoice = invoiceRepositoryPort.save(invoice);

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/duplicate")
                        .header("Authorization", authHeader))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-0002"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void testMarkPaidSuccess() throws Exception {
        Invoice invoice = Invoice.create(
                testUser.getId(), testClient.getId(), "INV-0001",
                LocalDate.now(), LocalDate.now().plusDays(30), "USD",
                Collections.singletonList(com.invoiceflow.domain.model.InvoiceItem.create("item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, null, null, null, null
        );
        invoice.markSent(); // Must be SENT to mark PAID
        invoice = invoiceRepositoryPort.save(invoice);

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/mark-paid")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").exists());
    }

    @Test
    void testDeleteInvoiceThrowsOnActiveInvoice() throws Exception {
        Invoice invoice = Invoice.create(
                testUser.getId(), testClient.getId(), "INV-0001",
                LocalDate.now(), LocalDate.now().plusDays(30), "USD",
                Collections.singletonList(com.invoiceflow.domain.model.InvoiceItem.create("item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, null, null, null, null
        );
        invoice.markSent();
        invoice = invoiceRepositoryPort.save(invoice);

        mockMvc.perform(delete("/api/v1/invoices/" + invoice.getId())
                        .header("Authorization", authHeader))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business Rule Violation"));
    }
}
