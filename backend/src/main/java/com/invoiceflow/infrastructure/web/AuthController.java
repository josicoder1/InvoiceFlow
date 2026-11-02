package com.invoiceflow.infrastructure.web;

import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.LoginUseCase;
import com.invoiceflow.domain.port.in.RefreshSessionUseCase;
import com.invoiceflow.domain.port.in.RegisterUserUseCase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshSessionUseCase refreshSessionUseCase;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUseCase loginUseCase,
            RefreshSessionUseCase refreshSessionUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshSessionUseCase = refreshSessionUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        User user = registerUserUseCase.register(new RegisterUserUseCase.Command(
                request.email(), request.password(), request.fullName()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDto.fromDomain(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response) {
        
        LoginUseCase.TokenResponse tokenResponse = loginUseCase.login(new LoginUseCase.Command(
                request.email(), request.password()
        ));

        setRefreshTokenCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.ok(new LoginResponseDto(tokenResponse.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = getRefreshTokenFromCookies(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LoginUseCase.TokenResponse tokenResponse = refreshSessionUseCase.refresh(
                new RefreshSessionUseCase.Command(refreshToken)
        );

        setRefreshTokenCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.ok(new LoginResponseDto(tokenResponse.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    private String getRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Change to true in production
        cookie.setPath("/api/v1/auth"); // Only send on auth routes
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Change to true in production
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    // Request/Response DTOs
    public record RegisterRequestDto(
            @NotBlank(message = "Email is required")
            @Email(message = "Must be a valid email address")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters long")
            String password,

            @NotBlank(message = "Full name is required")
            String fullName
    ) {}

    public record LoginRequestDto(
            @NotBlank(message = "Email is required")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record LoginResponseDto(String accessToken) {}

    public record UserResponseDto(
            String id,
            String email,
            String fullName,
            String businessName,
            String businessAddress,
            String businessLogoUrl,
            String taxId,
            String defaultCurrency,
            String plan,
            boolean chapaConnected,
            boolean remindersEnabled,
            int reminderDaysBeforeDue,
            int reminderDaysAfterDue
    ) {
        public static UserResponseDto fromDomain(User user) {
            return new UserResponseDto(
                    user.getId().toString(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getBusinessName(),
                    user.getBusinessAddress(),
                    user.getBusinessLogoUrl(),
                    user.getTaxId(),
                    user.getDefaultCurrency(),
                    user.getPlan().name(),
                    user.isChapaConnected(),
                    user.isRemindersEnabled(),
                    user.getReminderDaysBeforeDue(),
                    user.getReminderDaysAfterDue()
            );
        }
    }
}
