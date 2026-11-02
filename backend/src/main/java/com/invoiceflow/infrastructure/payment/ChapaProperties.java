package com.invoiceflow.infrastructure.payment;
 
import org.springframework.boot.context.properties.ConfigurationProperties;
 
@ConfigurationProperties(prefix = "payments.chapa")
public record ChapaProperties(
        String secretKey,
        String webhookSecret,
        String connectRedirectUrl
) {}
