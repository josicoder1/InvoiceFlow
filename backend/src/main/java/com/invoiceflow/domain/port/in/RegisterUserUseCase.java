package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.User;

public interface RegisterUserUseCase {
    User register(Command command);

    record Command(String email, String password, String fullName) {}
}
