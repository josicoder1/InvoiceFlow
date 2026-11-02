package com.invoiceflow.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {
    private UUID id;
    private String email;
    private String passwordHash;
    private String googleId;
    private String fullName;
    private String businessName;
    private String businessAddress;
    private String businessLogoUrl;
    private String taxId;
    private String defaultCurrency;
    private String chapaMerchantKey;
    private String stripeAccountId;
    private Plan plan;
    private boolean remindersEnabled;
    private int reminderDaysBeforeDue;
    private int reminderDaysAfterDue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Static factory method for registration
    public static User register(String email, String passwordHash, String fullName) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
        return new User(
                UUID.randomUUID(),
                email,
                passwordHash,
                null,
                fullName,
                null,
                null,
                null,
                null,
                "USD",
                null,
                null,
                Plan.FREE,
                true,
                3,
                7,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // Static factory method to reconstitute domain object from repository/database state
    public static User reconstitute(
            UUID id, String email, String passwordHash, String googleId, String fullName,
            String businessName, String businessAddress, String businessLogoUrl, String taxId,
            String defaultCurrency, String chapaMerchantKey, String stripeAccountId, Plan plan,
            boolean remindersEnabled, int reminderDaysBeforeDue, int reminderDaysAfterDue,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(id, email, passwordHash, googleId, fullName, businessName, businessAddress,
                businessLogoUrl, taxId, defaultCurrency, chapaMerchantKey, stripeAccountId, plan,
                remindersEnabled, reminderDaysBeforeDue, reminderDaysAfterDue, createdAt, updatedAt);
    }

    // Mutator for business profile update
    public void updateBusinessProfile(
            String fullName, String businessName, String businessAddress,
            String businessLogoUrl, String taxId, String defaultCurrency) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
        this.fullName = fullName;
        this.businessName = businessName;
        this.businessAddress = businessAddress;
        this.businessLogoUrl = businessLogoUrl;
        this.taxId = taxId;
        this.defaultCurrency = defaultCurrency != null ? defaultCurrency : "USD";
        this.updatedAt = LocalDateTime.now();
    }

    // Mutator to update subscription plan
    public void updatePlan(Plan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("Plan cannot be null");
        }
        this.plan = plan;
        this.updatedAt = LocalDateTime.now();
    }

    // Mutator to connect Chapa account
    public void connectChapa(String chapaMerchantKey) {
        this.chapaMerchantKey = chapaMerchantKey;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isChapaConnected() {
        return chapaMerchantKey != null && !chapaMerchantKey.isBlank();
    }

    public void updateNotificationSettings(boolean remindersEnabled, int reminderDaysBeforeDue, int reminderDaysAfterDue) {
        if (reminderDaysBeforeDue < 0 || reminderDaysAfterDue < 0) {
            throw new IllegalArgumentException("Reminder days cannot be negative");
        }
        this.remindersEnabled = remindersEnabled;
        this.reminderDaysBeforeDue = reminderDaysBeforeDue;
        this.reminderDaysAfterDue = reminderDaysAfterDue;
        this.updatedAt = LocalDateTime.now();
    }

}
