package com.invoiceflow.domain.model;

import com.invoiceflow.domain.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class InvoiceTest {

    @Test
    void testCreateInvoiceCalculatesTotalsWithPercentDiscountAndTax() {
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        InvoiceItem item1 = InvoiceItem.create("item 1", BigDecimal.valueOf(2), BigDecimal.valueOf(50), 1); // total = 100.00
        InvoiceItem item2 = InvoiceItem.create("item 2", BigDecimal.valueOf(3), BigDecimal.valueOf(20), 2); // total = 60.00

        Invoice invoice = Invoice.create(
                userId,
                clientId,
                "INV-0001",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "USD",
                Arrays.asList(item1, item2),
                DiscountType.PERCENT,
                BigDecimal.valueOf(10), // 10% discount -> subtotal 160, discount 16, discounted 144
                BigDecimal.valueOf(5),  // 5% tax -> 144 * 0.05 = 7.20 -> total 151.20
                "notes",
                "payment terms"
        );

        assertEquals(BigDecimal.valueOf(160).setScale(2), invoice.getSubtotal());
        assertEquals(BigDecimal.valueOf(151.20).setScale(2), invoice.getTotal());
        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
    }

    @Test
    void testCreateInvoiceCalculatesTotalsWithFixedDiscountAndTax() {
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        InvoiceItem item1 = InvoiceItem.create("item 1", BigDecimal.valueOf(1), BigDecimal.valueOf(100), 1); // total = 100.00

        Invoice invoice = Invoice.create(
                userId,
                clientId,
                "INV-0001",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "USD",
                Collections.singletonList(item1),
                DiscountType.FIXED,
                BigDecimal.valueOf(20), // 20 USD discount -> subtotal 100, discount 20, discounted 80
                BigDecimal.valueOf(10), // 10% tax -> 80 * 0.10 = 8.00 -> total 88.00
                "notes",
                "payment terms"
        );

        assertEquals(BigDecimal.valueOf(100).setScale(2), invoice.getSubtotal());
        assertEquals(BigDecimal.valueOf(88).setScale(2), invoice.getTotal());
    }

    @Test
    void testEditInvoiceMutatesFieldsAndCalculatesNewTotals() {
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Invoice invoice = Invoice.create(
                userId,
                clientId,
                "INV-0001",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "USD",
                Collections.singletonList(InvoiceItem.create("item 1", BigDecimal.valueOf(1), BigDecimal.valueOf(100), 1)),
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "notes",
                "payment terms"
        );

        InvoiceItem newItem = InvoiceItem.create("item new", BigDecimal.valueOf(2), BigDecimal.valueOf(150), 1); // 300.00

        invoice.edit(
                clientId,
                LocalDate.now(),
                LocalDate.now().plusDays(15),
                "EUR",
                Collections.singletonList(newItem),
                DiscountType.FIXED,
                BigDecimal.valueOf(50), // 300 - 50 = 250
                BigDecimal.valueOf(20), // 250 * 1.2 = 300
                "new notes",
                "new payment terms"
        );

        assertEquals("EUR", invoice.getCurrency());
        assertEquals(BigDecimal.valueOf(300).setScale(2), invoice.getSubtotal());
        assertEquals(BigDecimal.valueOf(300).setScale(2), invoice.getTotal());
        assertEquals(LocalDate.now().plusDays(15), invoice.getDueDate());
    }

    @Test
    void testStateTransitions() {
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Invoice invoice = Invoice.create(
                userId,
                clientId,
                "INV-0001",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "USD",
                Collections.singletonList(InvoiceItem.create("item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, null, null, null, null
        );

        // Can't mark paid directly from DRAFT
        assertThrows(BusinessRuleViolationException.class, () -> invoice.markPaid(null));

        // Transition: DRAFT -> SENT
        invoice.markSent();
        assertEquals(InvoiceStatus.SENT, invoice.getStatus());

        // Transition: SENT -> PAID
        invoice.markPaid(null);
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertNotNull(invoice.getPaidAt());

        // Can't edit or cancel after PAID
        assertThrows(BusinessRuleViolationException.class, () -> invoice.edit(clientId, LocalDate.now(), LocalDate.now(), "USD", null, null, null, null, null, null));
        assertThrows(BusinessRuleViolationException.class, invoice::cancel);
    }

    @Test
    void testIsPayableOnlyForSentOrOverdue() {
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Invoice invoice = Invoice.create(
                userId, clientId, "INV-0001", LocalDate.now(), LocalDate.now().plusDays(30), "USD",
                Collections.singletonList(InvoiceItem.create("item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, null, null, null, null);
        assertFalse(invoice.isPayable());
        invoice.markSent();
        assertTrue(invoice.isPayable());
        invoice.markOverdue();
        assertTrue(invoice.isPayable());
        invoice.markPaid(null);
        assertFalse(invoice.isPayable());
    }
}
