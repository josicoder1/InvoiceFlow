package com.invoiceflow.domain.port.in;

import java.util.UUID;

public interface DownloadProposalPdfUseCase {
    byte[] downloadPdf(Query query);

    record Query(UUID proposalId, UUID userId) {}
}
