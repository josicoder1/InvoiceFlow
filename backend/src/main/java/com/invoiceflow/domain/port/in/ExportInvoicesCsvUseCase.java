package com.invoiceflow.domain.port.in;

import java.util.UUID;

public interface ExportInvoicesCsvUseCase {
    byte[] exportCsv(Query query);

    record Query(UUID userId) {}
}
