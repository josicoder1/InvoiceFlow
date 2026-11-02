package com.invoiceflow.infrastructure.web;
 
import com.invoiceflow.domain.port.in.HandleChapaWebhookUseCase;
import com.invoiceflow.domain.port.in.ChapaConnectUseCase;
import com.invoiceflow.domain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
import java.util.Map;
 
@RestController
@RequestMapping("/api/v1/payments/chapa")
public class ChapaPaymentsController {
    private final ChapaConnectUseCase chapaConnectUseCase;
    private final HandleChapaWebhookUseCase handleChapaWebhookUseCase;
 
    public ChapaPaymentsController(
            ChapaConnectUseCase chapaConnectUseCase,
            HandleChapaWebhookUseCase handleChapaWebhookUseCase) {
        this.chapaConnectUseCase = chapaConnectUseCase;
        this.handleChapaWebhookUseCase = handleChapaWebhookUseCase;
    }
 
    @PostMapping("/connect")
    public ResponseEntity<Void> connect(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> request) {
        String chapaMerchantKey = request.get("chapaMerchantKey");
        chapaConnectUseCase.connectChapa(user.getId(), chapaMerchantKey);
        return ResponseEntity.ok().build();
    }
 
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(HttpServletRequest request, @RequestBody String payload) {
        String signature = request.getHeader("x-chapa-signature");
        if (signature == null) {
            signature = request.getHeader("Chapa-Signature");
        }
        handleChapaWebhookUseCase.handle(payload, signature);
        return ResponseEntity.ok().build();
    }
}
