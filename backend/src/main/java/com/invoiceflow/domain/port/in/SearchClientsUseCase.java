package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface SearchClientsUseCase {
    Page<Client> searchClients(Query query);

    record Query(UUID userId, String searchQuery, Pageable pageable) {}
}
