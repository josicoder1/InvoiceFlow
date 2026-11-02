package com.invoiceflow.application.service;
 
import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.port.in.ChapaConnectUseCase;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.UUID;
 
@Service
public class ChapaConnectService implements ChapaConnectUseCase {
    private final UserRepositoryPort userRepositoryPort;
 
    public ChapaConnectService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }
 
    @Override
    @Transactional
    public void connectChapa(UUID userId, String chapaMerchantKey) {
        if (chapaMerchantKey == null || chapaMerchantKey.isBlank()) {
            throw new IllegalArgumentException("Chapa secret key is required");
        }
        var user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.connectChapa(chapaMerchantKey);
        userRepositoryPort.save(user);
    }
}
