package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.DiscountType;
import com.invoiceflow.domain.model.Invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EditInvoiceUseCase {
    Invoice editInvoice(Command command);

    record Command(
            UUID invoiceId,
            UUID userId,
            UUID clientId,
            LocalDate issueDate,
            LocalDate dueDate,
            String currency,
            List<CreateInvoiceUseCase.ItemCommand> items,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal taxRate,
            String notes,
            String paymentTerms
    ) {}
}
