package com.invoiceflow.domain.port.in;
 
import java.util.UUID;
 
public interface ChapaConnectUseCase {
    void connectChapa(UUID userId, String chapaMerchantKey);
}
