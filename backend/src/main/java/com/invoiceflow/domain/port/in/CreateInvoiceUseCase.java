package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.DiscountType;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.Plan;
import com.invoiceflow.domain.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CreateInvoiceUseCase {
    Invoice createInvoice(Command command);

    record Command(
            User user,
            UUID userId,
            UUID clientId,
            LocalDate issueDate,
            LocalDate dueDate,
            String currency,
            List<ItemCommand> items,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal taxRate,
            String notes,
            String paymentTerms
    ) {}

    record ItemCommand(
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            int position
    ) {}
}
