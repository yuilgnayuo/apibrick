package com.citi.tts.apibrick.core.encryption;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.Security;
import java.util.Base64;

/**
 * Decryption Service - Provides AES-256-GCM decryption
 */
@Service
public class DecryptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DecryptionService.class);
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;
    
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    /**
     * Decrypt data using AES-256-GCM
     * 
     * @param encryptedDataBase64 Base64-encoded encrypted data (IV + ciphertext)
     * @param keyBase64 Base64-encoded decryption key
     * @return Decrypted plaintext
     */
    public String decrypt(String encryptedDataBase64, String keyBase64) {
        try {
            // Decode encrypted data
            byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);
            
            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[IV_SIZE];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);
            
            // Decode key
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            return new String(plaintext, "UTF-8");
            
        } catch (Exception e) {
            logger.error("Decryption error", e);
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }
}

