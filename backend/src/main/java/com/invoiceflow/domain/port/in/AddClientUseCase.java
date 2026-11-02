package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Plan;
import com.invoiceflow.domain.model.User;
import java.util.UUID;

public interface AddClientUseCase {
    Client addClient(Command command);

    record Command(User user, UUID userId, String name, String email, String company, String address, String currency) {}
}
