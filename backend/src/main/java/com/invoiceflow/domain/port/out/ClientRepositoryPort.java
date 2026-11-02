package com.invoiceflow.domain.port.out;

import com.invoiceflow.domain.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepositoryPort {
    Client save(Client client);
    Optional<Client> findById(UUID id);
    Optional<Client> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndEmail(UUID userId, String email);
    boolean existsByUserIdAndEmailAndIdNot(UUID userId, String email, UUID id);
    void delete(Client client);
    Page<Client> findByUserIdAndSearch(UUID userId, String query, Pageable pageable);
    long countByUserId(UUID userId);
}
