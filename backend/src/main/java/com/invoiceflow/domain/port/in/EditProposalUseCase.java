package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Proposal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EditProposalUseCase {
    Proposal editProposal(Command command);

    record Command(
            UUID proposalId, UUID userId, int version, UUID clientId, String title,
            String overview, String scope, String timeline,
            String pricingNotes, LocalDate validUntil,
            List<CreateProposalUseCase.ItemCommand> items,
            com.invoiceflow.domain.model.DiscountType discountType,
            BigDecimal discountValue, BigDecimal taxRate
    ) {}
}
