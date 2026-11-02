package com.invoiceflow.domain.port.in;

import java.util.UUID;

public interface DeleteInvoiceUseCase {
    void deleteInvoice(Command command);

    record Command(UUID invoiceId, UUID userId) {}
}
