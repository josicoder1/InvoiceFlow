package com.invoiceflow.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtKeyProvider {

    @Value("${security.jwt.private-key:}")
    private String privateKeyPem;

    @Value("${security.jwt.public-key:}")
    private String publicKeyPem;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            if (privateKeyPem == null || privateKeyPem.isBlank() ||
                publicKeyPem == null || publicKeyPem.isBlank()) {
                generateKeyPair();
            } else {
                loadKeys();
            }
        } catch (Exception e) {
            System.err.println("Failed to load JWT RS256 keys. Generating in-memory keys for development fallback...");
            generateKeyPair();
        }
    }

    private void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
            System.out.println("Successfully generated new RSA key pair for JWT signing.");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA algorithm not supported on this platform", e);
        }
    }

    private void loadKeys() throws Exception {
        String privateKeyClean = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        String publicKeyClean = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyClean);
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyClean);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        this.publicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        System.out.println("Successfully loaded RSA key pair from configuration.");
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
