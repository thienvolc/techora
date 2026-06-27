package com.techora.common.infra.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class CryptoService {

    private static final String SHA_256 = "SHA-256";
    private static final String HMAC_SHA_512 = "HmacSHA512";

    public String sha256(String value) {
        return HexFormat.of().formatHex(digest(value));
    }

    public String hmacSha512(String secret, String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_512);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_512));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA512 signing is unavailable", ex);
        }
    }

    private byte[] digest(String value) {
        try {
            return MessageDigest.getInstance(SHA_256).digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
