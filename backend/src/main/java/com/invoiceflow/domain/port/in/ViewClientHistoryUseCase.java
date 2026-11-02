package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Invoice;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ViewClientHistoryUseCase {
    ClientHistory viewHistory(Query query);

    record Query(UUID clientId, UUID userId) {}

    record ClientHistory(
            Client client,
            List<Invoice> invoices,
            BigDecimal totalBilled
    ) {}
}
