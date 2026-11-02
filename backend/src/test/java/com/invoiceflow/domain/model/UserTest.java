package com.invoiceflow.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testRegisterCreatesValidUser() {
        User user = User.register("test@example.com", "hashed_pass", "John Doe");

        assertNotNull(user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashed_pass", user.getPasswordHash());
        assertEquals("John Doe", user.getFullName());
        assertEquals(Plan.FREE, user.getPlan());
        assertEquals("USD", user.getDefaultCurrency());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void testRegisterThrowsExceptionOnEmptyEmail() {
        assertThrows(IllegalArgumentException.class, () -> 
            User.register("", "hashed_pass", "John Doe")
        );
    }

    @Test
    void testUpdateBusinessProfileMutatesFields() {
        User user = User.register("test@example.com", "hashed_pass", "John Doe");
        
        user.updateBusinessProfile(
                "Johnathan Doe",
                "Doe Consulting",
                "123 Main St",
                "/logo.png",
                "TX-12345",
                "EUR"
        );

        assertEquals("Johnathan Doe", user.getFullName());
        assertEquals("Doe Consulting", user.getBusinessName());
        assertEquals("123 Main St", user.getBusinessAddress());
        assertEquals("/logo.png", user.getBusinessLogoUrl());
        assertEquals("TX-12345", user.getTaxId());
        assertEquals("EUR", user.getDefaultCurrency());
    }

    @Test
    void testUpdatePlanChangesPlan() {
        User user = User.register("test@example.com", "hashed_pass", "John Doe");
        user.updatePlan(Plan.PRO);
        assertEquals(Plan.PRO, user.getPlan());
    }
}
