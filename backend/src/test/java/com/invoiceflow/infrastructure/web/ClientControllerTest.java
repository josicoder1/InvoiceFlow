package com.invoiceflow.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceflow.BaseIntegrationTest;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.RegisterUserUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.TokenProviderPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ClientControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private ClientRepositoryPort clientRepositoryPort;

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String authHeader;

    @BeforeEach
    void setUp() {
        // Create unique user for each test
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
    }

    @Test
    void testAddClientSuccess() throws Exception {
        ClientController.ClientRequestDto request = new ClientController.ClientRequestDto(
                "Acme Inc", "contact@acme.com", "Acme Co", "123 Business Way", "USD"
        );

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Acme Inc"))
                .andExpect(jsonPath("$.email").value("contact@acme.com"));
    }

    @Test
    void testGetClientsPaginationAndSearch() throws Exception {
        // Save two clients
        Client client1 = Client.create(testUser.getId(), "Apple Corp", "apple@corp.com", null, null, null);
        Client client2 = Client.create(testUser.getId(), "Banana Corp", "banana@corp.com", null, null, null);
        clientRepositoryPort.save(client1);
        clientRepositoryPort.save(client2);

        // Search for "Apple"
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", authHeader)
                        .param("search", "Apple")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Apple Corp"));
    }

    @Test
    void testDeleteClientSuccess() throws Exception {
        Client client = Client.create(testUser.getId(), "ToDelete", "delete@me.com", null, null, null);
        client = clientRepositoryPort.save(client);

        mockMvc.perform(delete("/api/v1/clients/" + client.getId())
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        assertFalse(clientRepositoryPort.findById(client.getId()).isPresent());
    }
}
