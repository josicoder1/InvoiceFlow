package com.invoiceflow.domain.port.in;

import java.util.UUID;

public interface PreviewInvoiceUseCase {
    String previewInvoice(Query query);
    record Query(UUID invoiceId, UUID userId) {}
}
