package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface SearchInvoicesUseCase {
    Page<Invoice> searchInvoices(Query query);

    record Query(
            UUID userId,
            InvoiceStatus status,
            UUID clientId,
            LocalDate startDate,
            LocalDate endDate,
            String searchQuery,
            Pageable pageable
    ) {}
}
