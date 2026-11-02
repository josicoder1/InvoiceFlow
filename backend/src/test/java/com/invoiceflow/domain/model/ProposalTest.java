package com.invoiceflow.domain.model;

import com.invoiceflow.domain.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProposalTest {

    private Proposal draftProposal(List<ProposalItem> items) {
        return Proposal.create(
                UUID.randomUUID(), UUID.randomUUID(), "PROP-0001", "Test Proposal",
                "Overview", "Scope", "Timeline", "Pricing notes",
                LocalDate.now().plusDays(30), items,
                DiscountType.PERCENT, BigDecimal.TEN, BigDecimal.valueOf(5));
    }

    @Test
    void test_createProposal_assignsCorrectInitialStatus() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = draftProposal(List.of(item));
        assertEquals(ProposalStatus.DRAFT, proposal.getStatus());
        assertNotNull(proposal.getPublicToken());
        assertEquals("PROP-0001", proposal.getProposalNumber());
    }

    @Test
    void test_sendProposal_transitionsStatusToSent() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = draftProposal(List.of(item));
        proposal.send();
        assertEquals(ProposalStatus.SENT, proposal.getStatus());
    }

    @Test
    void test_sendProposal_throwsWhenAlreadySent() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = draftProposal(List.of(item));
        proposal.send();
        assertThrows(BusinessRuleViolationException.class, proposal::send);
    }

    @Test
    void test_sendProposal_throwsWhenNoItems() {
        Proposal proposal = draftProposal(Collections.emptyList());
        assertThrows(BusinessRuleViolationException.class, proposal::send);
    }

    @Test
    void test_markAsViewed_transitionsStatusToViewed() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = draftProposal(List.of(item));
        proposal.send();
        assertTrue(proposal.recordViewed());
        assertEquals(ProposalStatus.VIEWED, proposal.getStatus());
        assertNotNull(proposal.getViewedAt());
    }

    @Test
    void test_markAsViewed_doesNotUpdateViewedAtIfAlreadyViewed() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = draftProposal(List.of(item));
        proposal.send();
        proposal.recordViewed();
        LocalDateTime firstViewedAt = proposal.getViewedAt();
        assertFalse(proposal.recordViewed());
        assertEquals(firstViewedAt, proposal.getViewedAt());
    }

    @Test
    void test_acceptProposal_transitionsStatusToAccepted() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = draftProposal(List.of(item));
        proposal.send();
        proposal.accept("data:image/png;base64,abc");
        assertEquals(ProposalStatus.ACCEPTED, proposal.getStatus());
        assertNotNull(proposal.getSignedAt());
    }

    @Test
    void test_acceptProposal_throwsWhenAlreadyAccepted() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = draftProposal(List.of(item));
        proposal.send();
        proposal.accept("data:image/png;base64,abc");
        assertThrows(BusinessRuleViolationException.class,
                () -> proposal.accept("data:image/png;base64,abc"));
    }

    @Test
    void test_acceptProposal_throwsWhenExpired() {
        Proposal proposal = Proposal.create(
                UUID.randomUUID(), UUID.randomUUID(), "PROP-0002", "Expired",
                null, null, null, null,
                LocalDate.now().minusDays(1), List.of(ProposalItem.create("X", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, BigDecimal.ZERO, BigDecimal.ZERO);
        proposal.send();
        assertThrows(BusinessRuleViolationException.class,
                () -> proposal.accept("data:image/png;base64,abc"));
    }

    @Test
    void test_declineProposal_transitionsStatusToDeclined() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = draftProposal(List.of(item));
        proposal.send();
        proposal.decline();
        assertEquals(ProposalStatus.DECLINED, proposal.getStatus());
    }

    @Test
    void test_declineProposal_throwsWhenAlreadyDeclined() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = draftProposal(List.of(item));
        proposal.send();
        proposal.decline();
        assertThrows(BusinessRuleViolationException.class, proposal::decline);
    }

    @Test
    void test_calculateTotal_withPercentDiscount() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(200), 1);
        Proposal proposal = Proposal.create(
                UUID.randomUUID(), UUID.randomUUID(), "PROP-0003", "Totals",
                null, null, null, null, null, List.of(item),
                DiscountType.PERCENT, BigDecimal.TEN, BigDecimal.valueOf(5));
        assertEquals(BigDecimal.valueOf(200).setScale(2), proposal.getSubtotal());
        assertEquals(BigDecimal.valueOf(189).setScale(2), proposal.getTotal());
    }

    @Test
    void test_calculateTotal_withFixedDiscount() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = Proposal.create(
                UUID.randomUUID(), UUID.randomUUID(), "PROP-0004", "Totals",
                null, null, null, null, null, List.of(item),
                DiscountType.FIXED, BigDecimal.valueOf(20), BigDecimal.valueOf(10));
        assertEquals(BigDecimal.valueOf(88).setScale(2), proposal.getTotal());
    }

    @Test
    void test_calculateTotal_withNoDiscount() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(50), 1);
        Proposal proposal = Proposal.create(
                UUID.randomUUID(), UUID.randomUUID(), "PROP-0005", "Totals",
                null, null, null, null, null, List.of(item),
                null, BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(BigDecimal.valueOf(50).setScale(2), proposal.getTotal());
    }

    @Test
    void test_convertToInvoice_throwsWhenNotAccepted() {
        ProposalItem item = ProposalItem.create("Design", BigDecimal.ONE, BigDecimal.valueOf(100), 1);
        Proposal proposal = draftProposal(List.of(item));
        assertThrows(BusinessRuleViolationException.class,
                () -> proposal.markConverted(UUID.randomUUID()));
    }
}
