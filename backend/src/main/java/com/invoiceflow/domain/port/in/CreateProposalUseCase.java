package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Proposal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CreateProposalUseCase {
    Proposal createProposal(Command command);

    record Command(
            UUID userId, UUID clientId, String title,
            String overview, String scope, String timeline,
            String pricingNotes, LocalDate validUntil,
            List<ItemCommand> items,
            com.invoiceflow.domain.model.DiscountType discountType,
            BigDecimal discountValue, BigDecimal taxRate
    ) {}

    record ItemCommand(String description, BigDecimal quantity, BigDecimal unitPrice, int position) {}
}
