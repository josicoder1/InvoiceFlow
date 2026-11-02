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
public class ProposalItem {
    private UUID id;
    private UUID proposalId;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
    private int position;

    public static ProposalItem create(String description, BigDecimal quantity, BigDecimal unitPrice, int position) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        if (quantity != null && unitPrice != null) {
            calculatedTotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        } else if (unitPrice != null) {
            calculatedTotal = unitPrice.setScale(2, RoundingMode.HALF_UP);
        }

        return new ProposalItem(
                UUID.randomUUID(),
                null, // Assigned when proposal is created
                description,
                quantity,
                unitPrice,
                calculatedTotal,
                position
        );
    }

    public static ProposalItem reconstitute(
            UUID id, UUID proposalId, String description, BigDecimal quantity,
            BigDecimal unitPrice, BigDecimal total, int position) {
        return new ProposalItem(id, proposalId, description, quantity, unitPrice, total, position);
    }

    public void assignProposal(UUID proposalId) {
        this.proposalId = proposalId;
    }
}
