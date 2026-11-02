package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Client;
import java.util.UUID;

public interface EditClientUseCase {
    Client editClient(Command command);

    record Command(UUID clientId, UUID userId, String name, String email, String company, String address, String currency) {}
}
