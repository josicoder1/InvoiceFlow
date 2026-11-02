package com.invoiceflow.domain.port.in;

public interface LoginUseCase {
    TokenResponse login(Command command);

    record Command(String email, String password) {}
    record TokenResponse(String accessToken, String refreshToken) {}
}
