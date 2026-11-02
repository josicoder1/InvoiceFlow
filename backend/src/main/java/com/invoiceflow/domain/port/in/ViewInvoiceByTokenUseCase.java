package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Invoice;

public interface ViewInvoiceByTokenUseCase {
    Invoice viewByToken(String publicToken);
}
