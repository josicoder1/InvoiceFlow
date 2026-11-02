package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Invoice;

import java.util.UUID;

public interface ConvertProposalToInvoiceUseCase {
    Invoice convertToInvoice(Command command);

    record Command(UUID proposalId, UUID userId) {}
}
