package com.invoiceflow.domain.port.in;

public interface MarkOverdueInvoicesUseCase {
    /**
     * Finds all SENT invoices whose due date is before today and marks them OVERDUE.
     * @return number of invoices transitioned to OVERDUE
     */
    int markOverdueInvoices();
}
