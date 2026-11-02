package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.UpdateNotificationSettingsUseCase;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationSettingsService implements UpdateNotificationSettingsUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public NotificationSettingsService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    @Transactional
    public User updateSettings(Command command) {
        User user = userRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.updateNotificationSettings(
                command.remindersEnabled(),
                command.reminderDaysBeforeDue(),
                command.reminderDaysAfterDue());
        return userRepositoryPort.save(user);
    }
}
