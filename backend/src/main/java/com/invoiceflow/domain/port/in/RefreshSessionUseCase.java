package com.invoiceflow.domain.port.in;

public interface RefreshSessionUseCase {
    LoginUseCase.TokenResponse refresh(Command command);

    record Command(String refreshToken) {}
}
