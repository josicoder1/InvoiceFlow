package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.User;

import java.util.UUID;

public interface UpdateNotificationSettingsUseCase {
    User updateSettings(Command command);

    record Command(
            UUID userId,
            boolean remindersEnabled,
            int reminderDaysBeforeDue,
            int reminderDaysAfterDue
    ) {}
}
