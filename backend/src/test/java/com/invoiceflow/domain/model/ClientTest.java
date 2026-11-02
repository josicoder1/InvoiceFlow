package com.invoiceflow.domain.model;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    @Test
    void testCreateClientSuccess() {
        UUID userId = UUID.randomUUID();
        Client client = Client.create(userId, "Acme Corp", "info@acme.com", "Acme Inc", "123 Industrial Rd", "USD");

        assertNotNull(client.getId());
        assertEquals(userId, client.getUserId());
        assertEquals("Acme Corp", client.getName());
        assertEquals("info@acme.com", client.getEmail());
        assertEquals("Acme Inc", client.getCompany());
        assertEquals("123 Industrial Rd", client.getAddress());
        assertEquals("USD", client.getCurrency());
        assertNotNull(client.getCreatedAt());
        assertNotNull(client.getUpdatedAt());
    }

    @Test
    void testCreateClientThrowsOnEmptyNameOrEmail() {
        UUID userId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () ->
                Client.create(userId, "", "info@acme.com", "Acme Inc", "123 Industrial Rd", "USD")
        );
        assertThrows(IllegalArgumentException.class, () ->
                Client.create(userId, "Acme Corp", " ", "Acme Inc", "123 Industrial Rd", "USD")
        );
    }

    @Test
    void testUpdateClientMutatesFields() {
        UUID userId = UUID.randomUUID();
        Client client = Client.create(userId, "Acme Corp", "info@acme.com", "Acme Inc", "123 Industrial Rd", "USD");

        client.update("Acme Ltd", "contact@acme.com", "Acme Global", "456 Corporate Blvd", "EUR");

        assertEquals("Acme Ltd", client.getName());
        assertEquals("contact@acme.com", client.getEmail());
        assertEquals("Acme Global", client.getCompany());
        assertEquals("456 Corporate Blvd", client.getAddress());
        assertEquals("EUR", client.getCurrency());
    }
}
