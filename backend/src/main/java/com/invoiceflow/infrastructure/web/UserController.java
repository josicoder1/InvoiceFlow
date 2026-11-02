package com.invoiceflow.infrastructure.web;

import com.invoiceflow.domain.model.User;

import com.invoiceflow.domain.port.in.UpdateBusinessProfileUseCase;
import com.invoiceflow.domain.port.in.UpdateNotificationSettingsUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UpdateBusinessProfileUseCase updateBusinessProfileUseCase;
    private final UpdateNotificationSettingsUseCase updateNotificationSettingsUseCase;

    public UserController(
            UpdateBusinessProfileUseCase updateBusinessProfileUseCase,
            UpdateNotificationSettingsUseCase updateNotificationSettingsUseCase) {
        this.updateBusinessProfileUseCase = updateBusinessProfileUseCase;
        this.updateNotificationSettingsUseCase = updateNotificationSettingsUseCase;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthController.UserResponseDto> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(AuthController.UserResponseDto.fromDomain(user));
    }

    @PutMapping("/me")
    public ResponseEntity<AuthController.UserResponseDto> updateMe(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequestDto request) {

        User updatedUser = updateBusinessProfileUseCase.updateProfile(new UpdateBusinessProfileUseCase.Command(
                user.getId(),
                request.fullName(),
                request.businessName(),
                request.businessAddress(),
                user.getBusinessLogoUrl(), // Keep existing logo URL for now
                request.taxId(),
                request.defaultCurrency()
        ));

        return ResponseEntity.ok(AuthController.UserResponseDto.fromDomain(updatedUser));
    }

    @GetMapping("/me/notifications")
    public ResponseEntity<NotificationSettingsDto> getNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(NotificationSettingsDto.fromDomain(user));
    }

    @PutMapping("/me/notifications")
    public ResponseEntity<NotificationSettingsDto> updateNotifications(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NotificationSettingsDto request) {
        User updated = updateNotificationSettingsUseCase.updateSettings(
                new UpdateNotificationSettingsUseCase.Command(
                        user.getId(),
                        request.remindersEnabled(),
                        request.reminderDaysBeforeDue(),
                        request.reminderDaysAfterDue()));
        return ResponseEntity.ok(NotificationSettingsDto.fromDomain(updated));
    }



    @PostMapping("/me/logo")
    public ResponseEntity<AuthController.UserResponseDto> uploadLogo(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            return ResponseEntity.badRequest().header("Error", "Only JPEG/PNG are allowed").build();
        }

        if (file.getSize() > 2 * 1024 * 1024) { // 2MB
            return ResponseEntity.badRequest().header("Error", "File exceeds 2MB limit").build();
        }

        // Save file locally for development
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get("uploads/logos");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName));

        String logoUrl = "/uploads/logos/" + fileName;

        User updatedUser = updateBusinessProfileUseCase.updateProfile(new UpdateBusinessProfileUseCase.Command(
                user.getId(),
                user.getFullName(),
                user.getBusinessName(),
                user.getBusinessAddress(),
                logoUrl,
                user.getTaxId(),
                user.getDefaultCurrency()
        ));

        return ResponseEntity.ok(AuthController.UserResponseDto.fromDomain(updatedUser));
    }

    public record UpdateProfileRequestDto(
            @NotBlank(message = "Full name is required")
            String fullName,
            String businessName,
            String businessAddress,
            String taxId,
            String defaultCurrency
    ) {}

    public record NotificationSettingsDto(
            boolean remindersEnabled,
            int reminderDaysBeforeDue,
            int reminderDaysAfterDue
    ) {
        public static NotificationSettingsDto fromDomain(User user) {
            return new NotificationSettingsDto(
                    user.isRemindersEnabled(),
                    user.getReminderDaysBeforeDue(),
                    user.getReminderDaysAfterDue());
        }
    }
}
