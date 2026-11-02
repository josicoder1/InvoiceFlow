package com.invoiceflow.infrastructure.pdf;

import com.invoiceflow.domain.model.*;
import com.invoiceflow.domain.port.out.PdfGeneratorPort;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

@Component
public class OpenPdfGeneratorAdapter implements PdfGeneratorPort {

    private static final Color BRAND_COLOR = new Color(79, 70, 229); // indigo-600
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color MID_GRAY = new Color(100, 100, 100);
    private static final Font HEADING_FONT = new Font(Font.HELVETICA, 22, Font.BOLD, BRAND_COLOR);
    private static final Font SUBHEADING_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.DARK_GRAY);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, MID_GRAY);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font TABLE_CELL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font TOTAL_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, BRAND_COLOR);

    @Override
    public byte[] generateInvoicePdf(Invoice invoice, Client client, User user) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            // Header: Invoice number + status
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{2f, 1f});
            headerTable.setSpacingAfter(20);

            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.addElement(new Phrase("INVOICE", HEADING_FONT));
            titleCell.addElement(new Phrase(invoice.getInvoiceNumber(), new Font(Font.HELVETICA, 13, Font.BOLD, Color.DARK_GRAY)));
            headerTable.addCell(titleCell);

            PdfPCell statusCell = new PdfPCell();
            statusCell.setBorder(Rectangle.NO_BORDER);
            statusCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            statusCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Font statusFont = new Font(Font.HELVETICA, 11, Font.BOLD, getStatusColor(invoice.getStatus()));
            statusCell.addElement(new Phrase("● " + invoice.getStatus().name(), statusFont));
            headerTable.addCell(statusCell);

            document.add(headerTable);

            // Divider
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            divider.setSpacingAfter(16);
            PdfPCell divCell = new PdfPCell();
            divCell.setBackgroundColor(BRAND_COLOR);
            divCell.setFixedHeight(3);
            divCell.setBorder(Rectangle.NO_BORDER);
            divider.addCell(divCell);
            document.add(divider);

            // From / To section
            PdfPTable fromToTable = new PdfPTable(2);
            fromToTable.setWidthPercentage(100);
            fromToTable.setSpacingAfter(20);

            PdfPCell fromCell = new PdfPCell();
            fromCell.setBorder(Rectangle.NO_BORDER);
            fromCell.setBackgroundColor(LIGHT_GRAY);
            fromCell.setPadding(12);
            fromCell.addElement(new Phrase("FROM", new Font(Font.HELVETICA, 8, Font.BOLD, MID_GRAY)));
            fromCell.addElement(new Phrase(nvl(user.getBusinessName(), user.getFullName()), SUBHEADING_FONT));
            if (user.getBusinessAddress() != null) fromCell.addElement(new Phrase(user.getBusinessAddress(), SMALL_FONT));
            if (user.getTaxId() != null) fromCell.addElement(new Phrase("Tax ID: " + user.getTaxId(), SMALL_FONT));
            fromCell.addElement(new Phrase(user.getEmail(), SMALL_FONT));
            fromToTable.addCell(fromCell);

            PdfPCell toCell = new PdfPCell();
            toCell.setBorder(Rectangle.NO_BORDER);
            toCell.setBackgroundColor(LIGHT_GRAY);
            toCell.setPadding(12);
            toCell.addElement(new Phrase("BILL TO", new Font(Font.HELVETICA, 8, Font.BOLD, MID_GRAY)));
            toCell.addElement(new Phrase(client.getName(), SUBHEADING_FONT));
            if (client.getCompany() != null) toCell.addElement(new Phrase(client.getCompany(), NORMAL_FONT));
            if (client.getAddress() != null) toCell.addElement(new Phrase(client.getAddress(), SMALL_FONT));
            toCell.addElement(new Phrase(client.getEmail(), SMALL_FONT));
            fromToTable.addCell(toCell);
            document.add(fromToTable);

            // Dates
            PdfPTable datesTable = new PdfPTable(3);
            datesTable.setWidthPercentage(100);
            datesTable.setSpacingAfter(20);
            addDateCell(datesTable, "ISSUE DATE", invoice.getIssueDate().toString());
            addDateCell(datesTable, "DUE DATE", invoice.getDueDate().toString());
            addDateCell(datesTable, "PAYMENT TERMS", nvl(invoice.getPaymentTerms(), "—"));
            document.add(datesTable);

            // Line items table
            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{4f, 1f, 1.5f, 1.5f});
            itemsTable.setSpacingAfter(12);

            addTableHeader(itemsTable, "DESCRIPTION");
            addTableHeader(itemsTable, "QTY");
            addTableHeader(itemsTable, "UNIT PRICE");
            addTableHeader(itemsTable, "TOTAL");

            boolean alt = false;
            for (InvoiceItem item : invoice.getItems()) {
                Color rowColor = alt ? LIGHT_GRAY : Color.WHITE;
                addItemRow(itemsTable, item, invoice.getCurrency(), rowColor);
                alt = !alt;
            }
            document.add(itemsTable);

            // Totals
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(45);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setSpacingAfter(20);

            String sym = getCurrencySymbol(invoice.getCurrency());
            addTotalRow(totalsTable, "Subtotal", sym + formatAmount(invoice.getSubtotal()), false);

            if (invoice.getDiscountValue() != null && invoice.getDiscountValue().compareTo(BigDecimal.ZERO) > 0) {
                String discLabel = invoice.getDiscountType() == DiscountType.PERCENT
                        ? "Discount (" + invoice.getDiscountValue().stripTrailingZeros().toPlainString() + "%)"
                        : "Discount";
                addTotalRow(totalsTable, discLabel, "-" + sym + formatAmount(calcDiscount(invoice)), false);
            }

            if (invoice.getTaxRate() != null && invoice.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Tax (" + invoice.getTaxRate().stripTrailingZeros().toPlainString() + "%)",
                        sym + formatAmount(calcTax(invoice)), false);
            }

            addTotalRow(totalsTable, "TOTAL", sym + formatAmount(invoice.getTotal()), true);
            document.add(totalsTable);

            // Notes
            if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
                document.add(new Phrase("Notes", SUBHEADING_FONT));
                document.add(new Phrase("\n" + invoice.getNotes(), NORMAL_FONT));
                document.add(Chunk.NEWLINE);
            }

            // Footer
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("Generated by InvoiceFlow", SMALL_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateProposalPdf(Proposal proposal, Client client, User user) {
        try {
            ProposalPdfLayout layout = resolveProposalLayout(proposal.getTitle());
            Color brandColor = getProposalBrandColor(layout);
            Font headingFont = new Font(Font.HELVETICA, 22, Font.BOLD, brandColor);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{2f, 1f});
            headerTable.setSpacingAfter(20);

            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.addElement(new Phrase("PROPOSAL", headingFont));
            titleCell.addElement(new Phrase(proposal.getTitle(), new Font(Font.HELVETICA, 13, Font.BOLD, Color.DARK_GRAY)));
            if (proposal.getProposalNumber() != null) {
                titleCell.addElement(new Phrase(proposal.getProposalNumber(), SMALL_FONT));
            }
            headerTable.addCell(titleCell);

            PdfPCell statusCell = new PdfPCell();
            statusCell.setBorder(Rectangle.NO_BORDER);
            statusCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            statusCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Font statusFont = new Font(Font.HELVETICA, 11, Font.BOLD, getProposalStatusColor(proposal.getStatus()));
            statusCell.addElement(new Phrase("● " + proposal.getStatus().name(), statusFont));
            headerTable.addCell(statusCell);
            document.add(headerTable);

            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            divider.setSpacingAfter(16);
            PdfPCell divCell = new PdfPCell();
            divCell.setBackgroundColor(brandColor);
            divCell.setFixedHeight(3);
            divCell.setBorder(Rectangle.NO_BORDER);
            divider.addCell(divCell);
            document.add(divider);

            PdfPTable fromToTable = new PdfPTable(2);
            fromToTable.setWidthPercentage(100);
            fromToTable.setSpacingAfter(20);

            PdfPCell fromCell = new PdfPCell();
            fromCell.setBorder(Rectangle.NO_BORDER);
            fromCell.setBackgroundColor(tint(brandColor, 0.92f));
            fromCell.setPadding(12);
            fromCell.addElement(new Phrase("FROM", new Font(Font.HELVETICA, 8, Font.BOLD, MID_GRAY)));
            fromCell.addElement(new Phrase(nvl(user.getBusinessName(), user.getFullName()), SUBHEADING_FONT));
            if (user.getBusinessAddress() != null) fromCell.addElement(new Phrase(user.getBusinessAddress(), SMALL_FONT));
            fromCell.addElement(new Phrase(user.getEmail(), SMALL_FONT));
            fromToTable.addCell(fromCell);

            PdfPCell toCell = new PdfPCell();
            toCell.setBorder(Rectangle.NO_BORDER);
            toCell.setBackgroundColor(tint(brandColor, 0.92f));
            toCell.setPadding(12);
            toCell.addElement(new Phrase("TO", new Font(Font.HELVETICA, 8, Font.BOLD, MID_GRAY)));
            toCell.addElement(new Phrase(client.getName(), SUBHEADING_FONT));
            if (client.getCompany() != null) toCell.addElement(new Phrase(client.getCompany(), NORMAL_FONT));
            if (client.getAddress() != null) toCell.addElement(new Phrase(client.getAddress(), SMALL_FONT));
            toCell.addElement(new Phrase(client.getEmail(), SMALL_FONT));
            fromToTable.addCell(toCell);
            document.add(fromToTable);

            if (proposal.getValidUntil() != null) {
                document.add(new Phrase("Valid Until: " + proposal.getValidUntil(), NORMAL_FONT));
                document.add(Chunk.NEWLINE);
            }

            addProposalContentSections(document, proposal, layout, brandColor);

            if (proposal.getItems() != null && !proposal.getItems().isEmpty()) {
                PdfPTable itemsTable = new PdfPTable(4);
                itemsTable.setWidthPercentage(100);
                itemsTable.setWidths(new float[]{4f, 1f, 1.5f, 1.5f});
                itemsTable.setSpacingAfter(12);
                addTableHeader(itemsTable, "DESCRIPTION", brandColor);
                addTableHeader(itemsTable, "QTY", brandColor);
                addTableHeader(itemsTable, "UNIT PRICE", brandColor);
                addTableHeader(itemsTable, "TOTAL", brandColor);
                boolean alt = false;
                for (ProposalItem item : proposal.getItems()) {
                    addProposalItemRow(itemsTable, item, alt ? tint(brandColor, 0.95f) : Color.WHITE);
                    alt = !alt;
                }
                document.add(itemsTable);

                PdfPTable totalsTable = new PdfPTable(2);
                totalsTable.setWidthPercentage(45);
                totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalsTable.setSpacingAfter(12);
                String sym = "$";
                Font totalFont = new Font(Font.HELVETICA, 12, Font.BOLD, brandColor);
                addTotalRow(totalsTable, "Subtotal", sym + formatAmount(proposal.getSubtotal()), false, totalFont, brandColor);
                if (proposal.getDiscountValue() != null && proposal.getDiscountValue().compareTo(BigDecimal.ZERO) > 0) {
                    String discLabel = proposal.getDiscountType() == DiscountType.PERCENT
                            ? "Discount (" + proposal.getDiscountValue().stripTrailingZeros().toPlainString() + "%)"
                            : "Discount";
                    addTotalRow(totalsTable, discLabel, "-" + sym + formatAmount(calcProposalDiscount(proposal)), false, totalFont, brandColor);
                }
                if (proposal.getTaxRate() != null && proposal.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
                    addTotalRow(totalsTable, "Tax (" + proposal.getTaxRate().stripTrailingZeros().toPlainString() + "%)",
                            sym + formatAmount(calcProposalTax(proposal)), false, totalFont, brandColor);
                }
                addTotalRow(totalsTable, "TOTAL", sym + formatAmount(proposal.getTotal()), true, totalFont, brandColor);
                document.add(totalsTable);
            }

            if (layout != ProposalPdfLayout.CREATIVE) {
                addTextSection(document, "Pricing Notes", proposal.getPricingNotes());
            } else if (proposal.getPricingNotes() != null && !proposal.getPricingNotes().isBlank()) {
                addAccentTextSection(document, "Pricing Notes", proposal.getPricingNotes(), brandColor);
            }

            document.add(Chunk.NEWLINE);
            document.add(new Phrase("Signature", SUBHEADING_FONT));
            if (proposal.getSignatureData() != null && !proposal.getSignatureData().isBlank()) {
                try {
                    Image sig = Image.getInstance(java.util.Base64.getDecoder().decode(
                            proposal.getSignatureData().replaceFirst("^data:image/png;base64,", "")));
                    sig.scaleToFit(200, 80);
                    document.add(sig);
                    document.add(new Phrase("Signed by " + client.getName()
                            + (proposal.getSignedAt() != null ? " on " + proposal.getSignedAt().toLocalDate() : ""),
                            SMALL_FONT));
                } catch (Exception ignored) {
                    document.add(new Phrase("Signed by " + client.getName(), SMALL_FONT));
                }
            } else {
                document.add(new Phrase("_________________________________________", NORMAL_FONT));
                document.add(new Phrase("Client signature", SMALL_FONT));
            }

            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("Generated by InvoiceFlow", SMALL_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate proposal PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateInvoiceHtml(Invoice invoice, Client client, User user) {
        String sym = getCurrencySymbol(invoice.getCurrency());
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body{font-family:Helvetica,Arial,sans-serif;color:#1a1a2e;margin:0;padding:32px;background:#f8f9ff;}");
        sb.append(".container{max-width:800px;margin:0 auto;background:#fff;border-radius:12px;padding:40px;box-shadow:0 4px 24px rgba(79,70,229,0.08);}");
        sb.append(".header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:24px;}");
        sb.append(".invoice-title{font-size:28px;font-weight:bold;color:#4f46e5;}");
        sb.append(".invoice-number{font-size:16px;color:#555;margin-top:4px;}");
        sb.append(".status{font-size:13px;font-weight:bold;padding:6px 14px;border-radius:20px;display:inline-block;}");
        sb.append(".divider{height:3px;background:#4f46e5;border:none;margin:0 0 24px 0;}");
        sb.append(".parties{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:24px;}");
        sb.append(".party{background:#f5f5ff;border-radius:8px;padding:16px;}");
        sb.append(".party-label{font-size:10px;font-weight:bold;color:#888;text-transform:uppercase;letter-spacing:1px;margin-bottom:6px;}");
        sb.append(".party-name{font-size:14px;font-weight:bold;color:#1a1a2e;margin-bottom:2px;}");
        sb.append(".party-detail{font-size:12px;color:#666;margin-bottom:2px;}");
        sb.append(".dates{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:24px;}");
        sb.append(".date-box{background:#f5f5ff;border-radius:8px;padding:12px;}");
        sb.append(".date-label{font-size:10px;font-weight:bold;color:#888;text-transform:uppercase;}");
        sb.append(".date-val{font-size:13px;font-weight:bold;color:#1a1a2e;margin-top:4px;}");
        sb.append("table{width:100%;border-collapse:collapse;margin-bottom:16px;}");
        sb.append("th{background:#4f46e5;color:#fff;padding:10px 12px;text-align:left;font-size:11px;text-transform:uppercase;}");
        sb.append("td{padding:9px 12px;border-bottom:1px solid #f0f0f0;font-size:13px;}");
        sb.append("tr:nth-child(even) td{background:#fafafe;}");
        sb.append(".totals{margin-left:auto;width:280px;margin-bottom:24px;}");
        sb.append(".total-row{display:flex;justify-content:space-between;padding:5px 0;font-size:13px;color:#555;}");
        sb.append(".total-final{display:flex;justify-content:space-between;padding:10px 0;font-size:16px;font-weight:bold;color:#4f46e5;border-top:2px solid #4f46e5;margin-top:6px;}");
        sb.append(".notes{background:#fffbf0;border-left:4px solid #f59e0b;padding:14px;border-radius:4px;font-size:13px;color:#555;margin-bottom:24px;}");
        sb.append(".footer{text-align:center;font-size:11px;color:#aaa;margin-top:32px;}");
        sb.append("</style></head><body><div class='container'>");

        // Header
        sb.append("<div class='header'><div>");
        sb.append("<div class='invoice-title'>INVOICE</div>");
        sb.append("<div class='invoice-number'>").append(invoice.getInvoiceNumber()).append("</div>");
        sb.append("</div><div><span class='status' style='").append(getStatusStyle(invoice.getStatus())).append("'>")
          .append(invoice.getStatus().name()).append("</span></div></div>");
        sb.append("<hr class='divider'>");

        // Parties
        sb.append("<div class='parties'><div class='party'><div class='party-label'>From</div>");
        sb.append("<div class='party-name'>").append(esc(nvl(user.getBusinessName(), user.getFullName()))).append("</div>");
        if (user.getBusinessAddress() != null) sb.append("<div class='party-detail'>").append(esc(user.getBusinessAddress())).append("</div>");
        if (user.getTaxId() != null) sb.append("<div class='party-detail'>Tax ID: ").append(esc(user.getTaxId())).append("</div>");
        sb.append("<div class='party-detail'>").append(esc(user.getEmail())).append("</div></div>");

        sb.append("<div class='party'><div class='party-label'>Bill To</div>");
        sb.append("<div class='party-name'>").append(esc(client.getName())).append("</div>");
        if (client.getCompany() != null) sb.append("<div class='party-detail'>").append(esc(client.getCompany())).append("</div>");
        if (client.getAddress() != null) sb.append("<div class='party-detail'>").append(esc(client.getAddress())).append("</div>");
        sb.append("<div class='party-detail'>").append(esc(client.getEmail())).append("</div></div></div>");

        // Dates
        sb.append("<div class='dates'>");
        sb.append("<div class='date-box'><div class='date-label'>Issue Date</div><div class='date-val'>").append(invoice.getIssueDate()).append("</div></div>");
        sb.append("<div class='date-box'><div class='date-label'>Due Date</div><div class='date-val'>").append(invoice.getDueDate()).append("</div></div>");
        sb.append("<div class='date-box'><div class='date-label'>Payment Terms</div><div class='date-val'>").append(esc(nvl(invoice.getPaymentTerms(), "—"))).append("</div></div>");
        sb.append("</div>");

        // Items table
        sb.append("<table><thead><tr><th>Description</th><th>Qty</th><th>Unit Price</th><th>Total</th></tr></thead><tbody>");
        for (InvoiceItem item : invoice.getItems()) {
            sb.append("<tr><td>").append(esc(item.getDescription())).append("</td>")
              .append("<td>").append(item.getQuantity().stripTrailingZeros().toPlainString()).append("</td>")
              .append("<td>").append(sym).append(formatAmount(item.getUnitPrice())).append("</td>")
              .append("<td>").append(sym).append(formatAmount(item.getTotal())).append("</td></tr>");
        }
        sb.append("</tbody></table>");

        // Totals
        sb.append("<div class='totals'>");
        sb.append("<div class='total-row'><span>Subtotal</span><span>").append(sym).append(formatAmount(invoice.getSubtotal())).append("</span></div>");
        if (invoice.getDiscountValue() != null && invoice.getDiscountValue().compareTo(BigDecimal.ZERO) > 0) {
            String dlabel = invoice.getDiscountType() == DiscountType.PERCENT
                ? "Discount (" + invoice.getDiscountValue().stripTrailingZeros().toPlainString() + "%)"
                : "Discount";
            sb.append("<div class='total-row'><span>").append(dlabel).append("</span><span>-").append(sym).append(formatAmount(calcDiscount(invoice))).append("</span></div>");
        }
        if (invoice.getTaxRate() != null && invoice.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<div class='total-row'><span>Tax (").append(invoice.getTaxRate().stripTrailingZeros().toPlainString())
              .append("%)</span><span>").append(sym).append(formatAmount(calcTax(invoice))).append("</span></div>");
        }
        sb.append("<div class='total-final'><span>TOTAL</span><span>").append(sym).append(formatAmount(invoice.getTotal())).append("</span></div>");
        sb.append("</div>");

        // Notes
        if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
            sb.append("<div class='notes'><strong>Notes:</strong> ").append(esc(invoice.getNotes())).append("</div>");
        }

        sb.append("<div class='footer'>Generated by InvoiceFlow</div>");
        sb.append("</div></body></html>");
        return sb.toString();
    }

    // ── helpers ──

    private enum ProposalPdfLayout { CLASSIC, CREATIVE, CORPORATE, EXECUTIVE }

    private ProposalPdfLayout resolveProposalLayout(String title) {
        if (title == null) return ProposalPdfLayout.CLASSIC;
        String lower = title.toLowerCase();
        if (lower.contains("web") || lower.contains("design")) return ProposalPdfLayout.CREATIVE;
        if (lower.contains("brand") || lower.contains("identity")) return ProposalPdfLayout.CORPORATE;
        if (lower.contains("retainer") || lower.contains("monthly")) return ProposalPdfLayout.EXECUTIVE;
        return ProposalPdfLayout.CLASSIC;
    }

    private Color getProposalBrandColor(ProposalPdfLayout layout) {
        return switch (layout) {
            case CREATIVE -> new Color(192, 38, 211);
            case CORPORATE -> new Color(8, 145, 178);
            case EXECUTIVE -> new Color(5, 150, 105);
            default -> BRAND_COLOR;
        };
    }

    private Color tint(Color color, float factor) {
        return new Color(
                Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor)),
                Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor)),
                Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor))
        );
    }

    private void addProposalContentSections(Document document, Proposal proposal, ProposalPdfLayout layout, Color brandColor)
            throws DocumentException {
        switch (layout) {
            case CREATIVE -> {
                addAccentTextSection(document, "Overview", proposal.getOverview(), brandColor);
                PdfPTable split = new PdfPTable(2);
                split.setWidthPercentage(100);
                split.setSpacingAfter(12);
                split.addCell(buildSectionCell("Scope of Work", proposal.getScope(), brandColor));
                split.addCell(buildSectionCell("Timeline", proposal.getTimeline(), brandColor));
                document.add(split);
            }
            case CORPORATE -> {
                addBorderedTextSection(document, "Overview", proposal.getOverview(), brandColor);
                addBorderedTextSection(document, "Scope of Work", proposal.getScope(), brandColor);
                addBorderedTextSection(document, "Timeline", proposal.getTimeline(), brandColor);
            }
            case EXECUTIVE -> {
                if (proposal.getOverview() != null && !proposal.getOverview().isBlank()) {
                    Font quoteFont = new Font(Font.HELVETICA, 14, Font.ITALIC, Color.DARK_GRAY);
                    Paragraph quote = new Paragraph(proposal.getOverview(), quoteFont);
                    quote.setSpacingAfter(14);
                    document.add(quote);
                }
                addTextSection(document, "Deliverables", proposal.getScope());
                addMilestoneSection(document, proposal.getTimeline(), brandColor);
            }
            default -> {
                addTextSection(document, "Overview", proposal.getOverview());
                addTextSection(document, "Scope of Work", proposal.getScope());
                addTextSection(document, "Timeline", proposal.getTimeline());
            }
        }
    }

    private PdfPCell buildSectionCell(String title, String text, Color brandColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(tint(brandColor, 0.7f));
        cell.setBackgroundColor(tint(brandColor, 0.94f));
        cell.setPadding(10);
        if (text != null && !text.isBlank()) {
            cell.addElement(new Phrase(title, new Font(Font.HELVETICA, 10, Font.BOLD, brandColor.darker())));
            cell.addElement(new Phrase("\n" + text, NORMAL_FONT));
        }
        return cell;
    }

    private void addAccentTextSection(Document document, String title, String text, Color brandColor) throws DocumentException {
        if (text == null || text.isBlank()) return;
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        box.setSpacingAfter(12);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(tint(brandColor, 0.93f));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(tint(brandColor, 0.75f));
        cell.setPadding(12);
        cell.addElement(new Phrase(title, new Font(Font.HELVETICA, 11, Font.BOLD, brandColor.darker())));
        cell.addElement(new Phrase("\n" + text, NORMAL_FONT));
        box.addCell(cell);
        document.add(box);
    }

    private void addBorderedTextSection(Document document, String title, String text, Color brandColor) throws DocumentException {
        if (text == null || text.isBlank()) return;
        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100);
        row.setWidths(new float[]{0.08f, 0.92f});
        row.setSpacingAfter(10);
        PdfPCell bar = new PdfPCell();
        bar.setBackgroundColor(brandColor);
        bar.setBorder(Rectangle.NO_BORDER);
        row.addCell(bar);
        PdfPCell content = new PdfPCell();
        content.setBorder(Rectangle.NO_BORDER);
        content.setPadding(8);
        content.addElement(new Phrase(title.toUpperCase(), new Font(Font.HELVETICA, 9, Font.BOLD, brandColor.darker())));
        content.addElement(new Phrase("\n" + text, NORMAL_FONT));
        row.addCell(content);
        document.add(row);
    }

    private void addMilestoneSection(Document document, String timeline, Color brandColor) throws DocumentException {
        if (timeline == null || timeline.isBlank()) return;
        document.add(new Phrase("Milestones", SUBHEADING_FONT));
        document.add(Chunk.NEWLINE);
        for (String line : timeline.split("\n")) {
            if (line.isBlank()) continue;
            Chunk chip = new Chunk("  " + line.trim() + "  ",
                    new Font(Font.HELVETICA, 9, Font.BOLD, brandColor.darker()));
            chip.setBackground(tint(brandColor, 0.9f));
            document.add(chip);
            document.add(new Phrase("   ", NORMAL_FONT));
        }
        document.add(Chunk.NEWLINE);
    }

    private void addDateCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setPadding(10);
        cell.addElement(new Phrase(label, new Font(Font.HELVETICA, 8, Font.BOLD, MID_GRAY)));
        cell.addElement(new Phrase(value, SUBHEADING_FONT));
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String text) {
        addTableHeader(table, text, BRAND_COLOR);
    }

    private void addTableHeader(PdfPTable table, String text, Color brandColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(brandColor);
        cell.setPadding(9);
        cell.setBorderColor(brandColor);
        table.addCell(cell);
    }

    private void addItemRow(PdfPTable table, InvoiceItem item, String currency, Color bgColor) {
        String sym = getCurrencySymbol(currency);
        String[] vals = {
            item.getDescription(),
            item.getQuantity().stripTrailingZeros().toPlainString(),
            sym + formatAmount(item.getUnitPrice()),
            sym + formatAmount(item.getTotal())
        };
        for (String val : vals) {
            PdfPCell cell = new PdfPCell(new Phrase(val, TABLE_CELL_FONT));
            cell.setBackgroundColor(bgColor);
            cell.setPadding(8);
            cell.setBorderColor(new Color(230, 230, 230));
            table.addCell(cell);
        }
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean isFinal) {
        addTotalRow(table, label, value, isFinal, TOTAL_FONT, BRAND_COLOR);
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean isFinal, Font totalFont, Color brandColor) {
        Font f = isFinal ? totalFont : NORMAL_FONT;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, f));
        labelCell.setBorder(isFinal ? Rectangle.TOP : Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        labelCell.setPaddingTop(isFinal ? 8 : 5);
        if (isFinal) labelCell.setBorderColor(brandColor);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, f));
        valueCell.setBorder(isFinal ? Rectangle.TOP : Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        valueCell.setPaddingTop(isFinal ? 8 : 5);
        if (isFinal) valueCell.setBorderColor(brandColor);
        table.addCell(valueCell);
    }

    private Color getStatusColor(InvoiceStatus status) {
        return switch (status) {
            case PAID -> new Color(16, 185, 129);
            case SENT -> new Color(59, 130, 246);
            case OVERDUE -> new Color(239, 68, 68);
            case CANCELLED -> new Color(249, 115, 22);
            default -> new Color(107, 114, 128);
        };
    }

    private Color getProposalStatusColor(ProposalStatus status) {
        return switch (status) {
            case ACCEPTED -> new Color(16, 185, 129);
            case SENT, VIEWED -> new Color(59, 130, 246);
            case DECLINED -> new Color(239, 68, 68);
            default -> new Color(107, 114, 128);
        };
    }

    private String getStatusStyle(InvoiceStatus status) {
        return switch (status) {
            case PAID -> "background:#d1fae5;color:#065f46;";
            case SENT -> "background:#dbeafe;color:#1e40af;";
            case OVERDUE -> "background:#fee2e2;color:#991b1b;";
            case CANCELLED -> "background:#ffedd5;color:#9a3412;";
            default -> "background:#f3f4f6;color:#374151;";
        };
    }

    private BigDecimal calcDiscount(Invoice invoice) {
        if (invoice.getDiscountValue() == null || invoice.getDiscountValue().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (invoice.getDiscountType() == DiscountType.PERCENT) {
            return invoice.getSubtotal()
                .multiply(invoice.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return invoice.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcTax(Invoice invoice) {
        BigDecimal discounted = invoice.getSubtotal().subtract(calcDiscount(invoice));
        if (discounted.compareTo(BigDecimal.ZERO) < 0) discounted = BigDecimal.ZERO;
        if (invoice.getTaxRate() == null) return BigDecimal.ZERO;
        return discounted.multiply(invoice.getTaxRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void addTextSection(Document document, String title, String text) throws DocumentException {
        if (text == null || text.isBlank()) return;
        document.add(new Phrase(title, SUBHEADING_FONT));
        document.add(new Phrase("\n" + text, NORMAL_FONT));
        document.add(Chunk.NEWLINE);
    }

    private void addProposalItemRow(PdfPTable table, ProposalItem item, Color bgColor) {
        String qty = item.getQuantity() != null ? item.getQuantity().stripTrailingZeros().toPlainString() : "—";
        String price = item.getUnitPrice() != null ? "$" + formatAmount(item.getUnitPrice()) : "—";
        String total = item.getTotal() != null ? "$" + formatAmount(item.getTotal()) : "—";
        String[] vals = { item.getDescription(), qty, price, total };
        for (String val : vals) {
            PdfPCell cell = new PdfPCell(new Phrase(val, TABLE_CELL_FONT));
            cell.setBackgroundColor(bgColor);
            cell.setPadding(8);
            cell.setBorderColor(new Color(230, 230, 230));
            table.addCell(cell);
        }
    }

    private BigDecimal calcProposalDiscount(Proposal proposal) {
        if (proposal.getDiscountValue() == null || proposal.getDiscountValue().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (proposal.getDiscountType() == DiscountType.PERCENT) {
            return proposal.getSubtotal()
                    .multiply(proposal.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return proposal.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcProposalTax(Proposal proposal) {
        BigDecimal discounted = proposal.getSubtotal().subtract(calcProposalDiscount(proposal));
        if (discounted.compareTo(BigDecimal.ZERO) < 0) discounted = BigDecimal.ZERO;
        if (proposal.getTaxRate() == null) return BigDecimal.ZERO;
        return discounted.multiply(proposal.getTaxRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%.2f", amount);
    }

    private String getCurrencySymbol(String currencyCode) {
        try {
            return Currency.getInstance(currencyCode).getSymbol(Locale.US);
        } catch (Exception e) {
            return currencyCode + " ";
        }
    }

    private String nvl(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
