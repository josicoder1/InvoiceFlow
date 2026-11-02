package com.invoiceflow.domain.port.out;

import java.util.Map;

public interface TokenProviderPort {
    String generateAccessToken(String subject, Map<String, Object> claims);
    String generateRefreshToken(String subject);
    String getSubjectFromToken(String token);
    boolean validateToken(String token);
}
