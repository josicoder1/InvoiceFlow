package com.invoiceflow.infrastructure.web;

import com.invoiceflow.BaseIntegrationTest;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.RegisterUserUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.TokenProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class DashboardControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RegisterUserUseCase registerUserUseCase;
    @Autowired private TokenProviderPort tokenProviderPort;
    @Autowired private ClientRepositoryPort clientRepositoryPort;

    private String authHeader;

    @BeforeEach
    void setUp() {
        String email = "dash_" + UUID.randomUUID() + "@example.com";
        User user = registerUserUseCase.register(new RegisterUserUseCase.Command(email, "password123", "Dash User"));
        String token = tokenProviderPort.generateAccessToken(user.getId().toString(), Map.of(
                "email", user.getEmail(), "fullName", user.getFullName(), "plan", user.getPlan().name()));
        authHeader = "Bearer " + token;

        Client client = Client.create(user.getId(), "Acme", "acme@example.com", null, null, "USD");
        clientRepositoryPort.save(client);
    }

    @Test
    void testGetSummaryReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.totalOutstanding").exists())
                .andExpect(jsonPath("$.activeClientsCount").value(1));
    }

    @Test
    void testGetRevenueChartReturns12Months() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/revenue-chart").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)));
    }

    @Test
    void testGetActivityReturnsAtMost10() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/activity").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(10))));
    }

    @Test
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testExportCsvReturnsCorrectHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export/csv").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("invoices.csv")))
                .andExpect(content().string(containsString("Invoice Number,Client,Status")));
    }
}
