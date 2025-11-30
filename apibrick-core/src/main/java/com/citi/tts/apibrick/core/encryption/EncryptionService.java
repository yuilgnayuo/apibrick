package com.citi.tts.apibrick.core.encryption;

import com.citi.tts.apibrick.common.exception.EncryptionException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * Encryption Service - Provides AES-256-GCM encryption
 * 
 * Features:
 * - AES-256-GCM symmetric encryption
 * - Secure random IV generation
 * - Base64 encoding for transport
 */
@Service
public class EncryptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12; // 96 bits for GCM
    private static final int TAG_SIZE = 128; // 128 bits authentication tag
    
    static {
        // Register BouncyCastle provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    /**
     * Encrypt data using AES-256-GCM
     * 
     * @param plaintext Data to encrypt
     * @param keyBase64 Base64-encoded encryption key
     * @return Base64-encoded encrypted data (IV + ciphertext)
     */
    public String encrypt(String plaintext, String keyBase64) {
        try {
            // Decode key
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            
            // Generate random IV
            byte[] iv = new byte[IV_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            
            // Combine IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);
            
            // Return Base64-encoded result
            return Base64.getEncoder().encodeToString(byteBuffer.array());
            
        } catch (Exception e) {
            logger.error("Encryption error", e);
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }
    
    /**
     * Generate a new encryption key
     * 
     * @return Base64-encoded encryption key
     */
    public String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(KEY_SIZE);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            logger.error("Key generation error", e);
            throw new EncryptionException("Failed to generate encryption key", e);
        }
    }
}

