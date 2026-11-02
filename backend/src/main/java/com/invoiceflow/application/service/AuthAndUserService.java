package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.BusinessRuleViolationException;
import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.*;
import com.invoiceflow.domain.port.out.PasswordEncoderPort;
import com.invoiceflow.domain.port.out.TokenProviderPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthAndUserService implements
        RegisterUserUseCase,
        LoginUseCase,
        RefreshSessionUseCase,
        UpdateBusinessProfileUseCase,
        GetUserProfileUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenProviderPort tokenProviderPort;

    public AuthAndUserService(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoderPort passwordEncoderPort,
            TokenProviderPort tokenProviderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.tokenProviderPort = tokenProviderPort;
    }

    @Override
    @Transactional
    public User register(RegisterUserUseCase.Command command) {
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new BusinessRuleViolationException("Email address already registered");
        }
        String passwordHash = passwordEncoderPort.encode(command.password());
        User user = User.register(command.email(), passwordHash, command.fullName());
        return userRepositoryPort.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginUseCase.Command command) {
        User user = userRepositoryPort.findByEmail(command.email())
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid email or password"));

        if (!passwordEncoderPort.matches(command.password(), user.getPasswordHash())) {
            throw new BusinessRuleViolationException("Invalid email or password");
        }

        String accessToken = tokenProviderPort.generateAccessToken(user.getId().toString(), Map.of(
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "plan", user.getPlan().name()
        ));
        String refreshToken = tokenProviderPort.generateRefreshToken(user.getId().toString());

        return new TokenResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshSessionUseCase.Command command) {
        if (!tokenProviderPort.validateToken(command.refreshToken())) {
            throw new BusinessRuleViolationException("Invalid or expired refresh token");
        }

        String userIdStr = tokenProviderPort.getSubjectFromToken(command.refreshToken());
        UUID userId = UUID.fromString(userIdStr);
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accessToken = tokenProviderPort.generateAccessToken(user.getId().toString(), Map.of(
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "plan", user.getPlan().name()
        ));
        String refreshToken = tokenProviderPort.generateRefreshToken(user.getId().toString());

        return new TokenResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public User updateProfile(UpdateBusinessProfileUseCase.Command command) {
        User user = userRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.updateBusinessProfile(
                command.fullName(),
                command.businessName(),
                command.businessAddress(),
                command.businessLogoUrl(),
                command.taxId(),
                command.defaultCurrency()
        );

        return userRepositoryPort.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
