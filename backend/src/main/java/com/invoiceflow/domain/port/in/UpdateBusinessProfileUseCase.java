package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.User;
import java.util.UUID;

public interface UpdateBusinessProfileUseCase {
    User updateProfile(Command command);

    record Command(
        UUID userId,
        String fullName,
        String businessName,
        String businessAddress,
        String businessLogoUrl,
        String taxId,
        String defaultCurrency
    ) {}
}
