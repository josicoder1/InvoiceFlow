package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.BusinessRuleViolationException;
import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceItem;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.model.Plan;
import com.invoiceflow.domain.port.in.*;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceService implements
        CreateInvoiceUseCase,
        EditInvoiceUseCase,
        DeleteInvoiceUseCase,
        DuplicateInvoiceUseCase,
        MarkInvoicePaidUseCase,
        MarkInvoiceSentUseCase,
        CancelInvoiceUseCase,
        SearchInvoicesUseCase,
        GetInvoiceUseCase {

    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;

    public InvoiceService(InvoiceRepositoryPort invoiceRepositoryPort, ClientRepositoryPort clientRepositoryPort) {
        this.invoiceRepositoryPort = invoiceRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
    }

    @Override
    @Transactional
    public Invoice createInvoice(CreateInvoiceUseCase.Command command) {
        if (command.user().getPlan() == Plan.FREE) {
            YearMonth currentMonth = YearMonth.now();
            LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
            long count = invoiceRepositoryPort.countByUserIdAndCreatedAtBetween(
                    command.userId(), monthStart, monthEnd);
            if (count >= 5) {
                throw new BusinessRuleViolationException(
                        "Free plan allows max 5 invoices per month. Upgrade to STARTER for unlimited.");
            }
        }

        // Enforce that client exists and belongs to the user
        clientRepositoryPort.findByIdAndUserId(command.clientId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        String invoiceNumber = generateNextInvoiceNumber(command.userId());

        List<InvoiceItem> items = command.items().stream()
                .map(item -> InvoiceItem.create(
                        item.description(),
                        item.quantity(),
                        item.unitPrice(),
                        item.position()
                ))
                .collect(Collectors.toList());

        Invoice invoice = Invoice.create(
                command.userId(),
                command.clientId(),
                invoiceNumber,
                command.issueDate(),
                command.dueDate(),
                command.currency(),
                items,
                command.discountType(),
                command.discountValue(),
                command.taxRate(),
                command.notes(),
                command.paymentTerms()
        );

        return invoiceRepositoryPort.save(invoice);
    }

    @Override
    @Transactional
    public Invoice editInvoice(EditInvoiceUseCase.Command command) {
        Invoice invoice = invoiceRepositoryPort.findByIdAndUserId(command.invoiceId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        clientRepositoryPort.findByIdAndUserId(command.clientId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        List<InvoiceItem> items = command.items().stream()
                .map(item -> InvoiceItem.create(
                        item.description(),
                        item.quantity(),
                        item.unitPrice(),
                        item.position()
                ))
                .collect(Collectors.toList());

        invoice.edit(
                command.clientId(),
                command.issueDate(),
                command.dueDate(),
                command.currency(),
                items,
                command.discountType(),
                command.discountValue(),
                command.taxRate(),
                command.notes(),
                command.paymentTerms()
        );

        return invoiceRepositoryPort.save(invoice);
    }

    @Override
    @Transactional
    public void deleteInvoice(DeleteInvoiceUseCase.Command command) {
        Invoice invoice = invoiceRepositoryPort.findByIdAndUserId(command.invoiceId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.DRAFT && invoice.getStatus() != InvoiceStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Can only delete invoices in DRAFT or CANCELLED status");
        }

        invoiceRepositoryPort.delete(invoice);
    }

    @Override
    @Transactional
    public Invoice duplicateInvoice(DuplicateInvoiceUseCase.Command command) {
        Invoice invoice = invoiceRepositoryPort.findByIdAndUserId(command.invoiceId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        String newInvoiceNumber = generateNextInvoiceNumber(command.userId());
        Invoice duplicatedInvoice = invoice.duplicate(newInvoiceNumber);

        return invoiceRepositoryPort.save(duplicatedInvoice);
    }

    @Override
    @Transactional
    public Invoice markPaid(MarkInvoicePaidUseCase.Command command) {
        Invoice invoice = invoiceRepositoryPort.findByIdAndUserId(command.invoiceId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        invoice.markPaid(LocalDateTime.now());
        return invoiceRepositoryPort.save(invoice);
    }

    @Override
    @Transactional
    public Invoice markSent(MarkInvoiceSentUseCase.Command command) {
        Invoice invoice = invoiceRepositoryPort.findByIdAndUserId(command.invoiceId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        invoice.markSent();
        return invoiceRepositoryPort.save(invoice);
    }

    @Override
    @Transactional
    public Invoice cancelInvoice(CancelInvoiceUseCase.Command command) {
        Invoice invoice = invoiceRepositoryPort.findByIdAndUserId(command.invoiceId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        invoice.cancel();
        return invoiceRepositoryPort.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Invoice> searchInvoices(SearchInvoicesUseCase.Query query) {
        return invoiceRepositoryPort.findByUserIdAndFilters(
                query.userId(),
                query.status(),
                query.clientId(),
                query.startDate(),
                query.endDate(),
                query.searchQuery(),
                query.pageable()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getInvoice(GetInvoiceUseCase.Query query) {
        return invoiceRepositoryPort.findByIdAndUserId(query.invoiceId(), query.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    private String generateNextInvoiceNumber(UUID userId) {
        Optional<String> maxNumberOpt = invoiceRepositoryPort.findMaxInvoiceNumberByUserId(userId);
        if (maxNumberOpt.isEmpty()) {
            return "INV-0001";
        }
        String maxNumber = maxNumberOpt.get();
        if (maxNumber.matches("^INV-\\d+$")) {
            try {
                int num = Integer.parseInt(maxNumber.substring(4));
                return String.format("INV-%04d", num + 1);
            } catch (NumberFormatException e) {
                return "INV-0001";
            }
        }
        return "INV-0001";
    }
}
