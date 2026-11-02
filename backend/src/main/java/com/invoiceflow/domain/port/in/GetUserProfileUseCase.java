package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.User;
import java.util.UUID;

public interface GetUserProfileUseCase {
    User getUserById(UUID id);
}
