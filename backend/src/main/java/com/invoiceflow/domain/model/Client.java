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
public class Client {
    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private String company;
    private String address;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Client create(UUID userId, String name, String email, String company, String address, String currency) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Client name cannot be empty");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Client email cannot be empty");
        }
        return new Client(
                UUID.randomUUID(),
                userId,
                name,
                email,
                company,
                address,
                currency != null ? currency : "USD",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public static Client reconstitute(
            UUID id, UUID userId, String name, String email, String company,
            String address, String currency, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Client(id, userId, name, email, company, address, currency, createdAt, updatedAt);
    }

    public void update(String name, String email, String company, String address, String currency) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Client name cannot be empty");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Client email cannot be empty");
        }
        this.name = name;
        this.email = email;
        this.company = company;
        this.address = address;
        this.currency = currency != null ? currency : "USD";
        this.updatedAt = LocalDateTime.now();
    }
}
