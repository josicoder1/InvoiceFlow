package com.invoiceflow.domain.port.in;

import java.util.UUID;

public interface DownloadInvoicePdfUseCase {
    byte[] downloadPdf(Query query);
    record Query(UUID invoiceId, UUID userId) {}
}
