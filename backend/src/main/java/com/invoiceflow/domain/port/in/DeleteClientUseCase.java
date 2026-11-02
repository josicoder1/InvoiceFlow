package com.invoiceflow.domain.port.in;

import java.util.UUID;

public interface DeleteClientUseCase {
    void deleteClient(Command command);

    record Command(UUID clientId, UUID userId) {}
}
