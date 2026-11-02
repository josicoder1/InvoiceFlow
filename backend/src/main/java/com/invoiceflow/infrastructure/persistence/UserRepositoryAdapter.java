package com.invoiceflow.infrastructure.persistence;

import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity savedEntity = userJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    private UserJpaEntity toEntity(User domain) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setEmail(domain.getEmail());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setGoogleId(domain.getGoogleId());
        entity.setFullName(domain.getFullName());
        entity.setBusinessName(domain.getBusinessName());
        entity.setBusinessAddress(domain.getBusinessAddress());
        entity.setBusinessLogoUrl(domain.getBusinessLogoUrl());
        entity.setTaxId(domain.getTaxId());
        entity.setDefaultCurrency(domain.getDefaultCurrency());
        entity.setChapaMerchantKey(domain.getChapaMerchantKey());
        entity.setStripeAccountId(domain.getStripeAccountId());
        entity.setPlan(domain.getPlan());
        entity.setRemindersEnabled(domain.isRemindersEnabled());
        entity.setReminderDaysBeforeDue(domain.getReminderDaysBeforeDue());
        entity.setReminderDaysAfterDue(domain.getReminderDaysAfterDue());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getGoogleId(),
                entity.getFullName(),
                entity.getBusinessName(),
                entity.getBusinessAddress(),
                entity.getBusinessLogoUrl(),
                entity.getTaxId(),
                entity.getDefaultCurrency(),
                entity.getChapaMerchantKey(),
                entity.getStripeAccountId(),
                entity.getPlan(),
                entity.isRemindersEnabled(),
                entity.getReminderDaysBeforeDue(),
                entity.getReminderDaysAfterDue(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
