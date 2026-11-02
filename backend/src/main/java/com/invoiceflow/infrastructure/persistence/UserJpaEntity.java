package com.invoiceflow.infrastructure.persistence;

import com.invoiceflow.domain.model.Plan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "business_address")
    private String businessAddress;

    @Column(name = "business_logo_url")
    private String businessLogoUrl;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "default_currency")
    private String defaultCurrency;

    @Column(name = "chapa_merchant_key")
    private String chapaMerchantKey;

    @Column(name = "stripe_account_id")
    private String stripeAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Column(name = "reminders_enabled", nullable = false)
    private boolean remindersEnabled = true;

    @Column(name = "reminder_days_before_due", nullable = false)
    private int reminderDaysBeforeDue = 3;

    @Column(name = "reminder_days_after_due", nullable = false)
    private int reminderDaysAfterDue = 7;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
