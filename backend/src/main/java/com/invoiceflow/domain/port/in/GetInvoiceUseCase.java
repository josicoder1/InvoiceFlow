package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Invoice;
import java.util.UUID;

public interface GetInvoiceUseCase {
    Invoice getInvoice(Query query);

    record Query(UUID invoiceId, UUID userId) {}
}
