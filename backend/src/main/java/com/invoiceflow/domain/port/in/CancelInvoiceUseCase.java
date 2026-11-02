package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Invoice;
import java.util.UUID;

public interface CancelInvoiceUseCase {
    Invoice cancelInvoice(Command command);

    record Command(UUID invoiceId, UUID userId) {}
}
