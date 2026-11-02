package com.invoiceflow.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceflow.BaseIntegrationTest;
import com.invoiceflow.domain.model.Plan;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.RegisterUserUseCase;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class SubscriptionControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RegisterUserUseCase registerUserUseCase;
    @Autowired private TokenProviderPort tokenProviderPort;

    private String authHeader;

    @BeforeEach
    void setUp() {
        String email = "sub_" + UUID.randomUUID() + "@example.com";
        User user = registerUserUseCase.register(new RegisterUserUseCase.Command(email, "password123", "Sub User"));
        String token = tokenProviderPort.generateAccessToken(user.getId().toString(), Map.of(
                "email", user.getEmail(), "fullName", user.getFullName(), "plan", user.getPlan().name()));
        authHeader = "Bearer " + token;
    }

    @Test
    void testGetSubscriptionReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/subscription").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("FREE")))
                .andExpect(jsonPath("$.invoicesThisMonth").exists())
                .andExpect(jsonPath("$.totalClients").exists());
    }

    @Test
    void testUpgradeToStarter() throws Exception {
        mockMvc.perform(post("/api/v1/subscription/upgrade")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("plan", "STARTER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("STARTER")));
    }

    @Test
    void testUpgradeToPro() throws Exception {
        mockMvc.perform(post("/api/v1/subscription/upgrade")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("plan", "PRO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("PRO")));
    }

    @Test
    void testCancelSubscription() throws Exception {
        mockMvc.perform(post("/api/v1/subscription/upgrade")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("plan", "STARTER"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/subscription/cancel").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("FREE")));
    }

    @Test
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/subscription"))
                .andExpect(status().isForbidden());
    }
}
