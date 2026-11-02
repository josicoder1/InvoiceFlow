package com.invoiceflow.infrastructure.web;

import com.invoiceflow.BaseIntegrationTest;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceItem;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.RegisterUserUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class PublicInvoiceControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RegisterUserUseCase registerUserUseCase;
    @Autowired private ClientRepositoryPort clientRepositoryPort;
    @Autowired private InvoiceRepositoryPort invoiceRepositoryPort;
    @Autowired private UserRepositoryPort userRepositoryPort;

    private String publicToken;

    @BeforeEach
    void setUp() {
        User user = registerUserUseCase.register(new RegisterUserUseCase.Command(
                "inv_" + UUID.randomUUID() + "@example.com", "password123", "Owner"));
        user.connectChapa("CHASECK_TEST-12345");
        userRepositoryPort.save(user);

        Client client = clientRepositoryPort.save(Client.create(
                user.getId(), "Client Co", "client@example.com", null, null, null));

        Invoice invoice = Invoice.create(
                user.getId(), client.getId(), "INV-0001",
                LocalDate.now(), LocalDate.now().plusDays(14), "USD",
                Collections.singletonList(InvoiceItem.create("Work", BigDecimal.ONE, BigDecimal.valueOf(100), 1)),
                null, null, null, null, null);
        invoice.ensurePublicToken();
        invoice.markSent();
        publicToken = invoice.getPublicToken();
        invoiceRepositoryPort.save(invoice);
    }

    @Test
    void testViewPublicInvoice() throws Exception {
        mockMvc.perform(get("/api/v1/public/invoices/" + publicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-0001"))
                .andExpect(jsonPath("$.canPay").value(true));
    }

    @Test
    void testCreateCheckoutSession() throws Exception {
        mockMvc.perform(post("/api/v1/public/invoices/" + publicToken + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").exists());
    }

    @Test
    void testConfirmPaymentMarksPaid() throws Exception {
        mockMvc.perform(post("/api/v1/public/invoices/" + publicToken + "/confirm-payment")
                        .contentType("application/json")
                        .content("{\"txRef\":\"tx-test-confirm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPaid").value(true))
                .andExpect(jsonPath("$.status").value("PAID"));
    }
}
