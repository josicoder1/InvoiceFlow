package com.invoiceflow.infrastructure.persistence;

import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ClientRepositoryAdapter implements ClientRepositoryPort {

    private final ClientJpaRepository clientJpaRepository;

    public ClientRepositoryAdapter(ClientJpaRepository clientJpaRepository) {
        this.clientJpaRepository = clientJpaRepository;
    }

    @Override
    public Client save(Client client) {
        ClientJpaEntity entity = toEntity(client);
        ClientJpaEntity savedEntity = clientJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Client> findById(UUID id) {
        return clientJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Client> findByIdAndUserId(UUID id, UUID userId) {
        return clientJpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public boolean existsByUserIdAndEmail(UUID userId, String email) {
        return clientJpaRepository.existsByUserIdAndEmail(userId, email);
    }

    @Override
    public boolean existsByUserIdAndEmailAndIdNot(UUID userId, String email, UUID id) {
        return clientJpaRepository.existsByUserIdAndEmailAndIdNot(userId, email, id);
    }

    @Override
    public void delete(Client client) {
        clientJpaRepository.delete(toEntity(client));
    }

    @Override
    public Page<Client> findByUserIdAndSearch(UUID userId, String query, Pageable pageable) {
        return clientJpaRepository.findByUserIdAndSearch(userId, query, pageable).map(this::toDomain);
    }

    @Override
    public long countByUserId(UUID userId) {
        return clientJpaRepository.countByUserId(userId);
    }

    private ClientJpaEntity toEntity(Client domain) {
        ClientJpaEntity entity = new ClientJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setName(domain.getName());
        entity.setEmail(domain.getEmail());
        entity.setCompany(domain.getCompany());
        entity.setAddress(domain.getAddress());
        entity.setCurrency(domain.getCurrency());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private Client toDomain(ClientJpaEntity entity) {
        return Client.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                entity.getEmail(),
                entity.getCompany(),
                entity.getAddress(),
                entity.getCurrency(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
