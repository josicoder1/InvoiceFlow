package com.invoiceflow.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InvoiceItem {
    private UUID id;
    private UUID invoiceId;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
    private int position;

    public static InvoiceItem create(String description, BigDecimal quantity, BigDecimal unitPrice, int position) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        BigDecimal calculatedTotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        return new InvoiceItem(
                UUID.randomUUID(),
                null, // Assigned when invoice is created
                description,
                quantity,
                unitPrice,
                calculatedTotal,
                position
        );
    }

    public static InvoiceItem reconstitute(
            UUID id, UUID invoiceId, String description, BigDecimal quantity,
            BigDecimal unitPrice, BigDecimal total, int position) {
        return new InvoiceItem(id, invoiceId, description, quantity, unitPrice, total, position);
    }

    // Set parent invoice ID context
    public void assignInvoice(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }
}
