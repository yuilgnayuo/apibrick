package com.citi.tts.apibrick.core.encryption;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

/**
 * Data Masking Service - Automatically masks sensitive fields
 * 
 * Supports:
 * - Phone numbers (keep last 4 digits)
 * - ID card numbers (keep last 4 digits)
 * - Email addresses (mask domain)
 * - Bank card numbers (keep last 4 digits)
 */
@Service
public class DataMaskingService {
    
    // Patterns for sensitive data detection
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{16,19}$");
    
    /**
     * Mask sensitive data based on detected type
     * 
     * @param value Original value
     * @return Masked value
     */
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        String trimmed = value.trim();
        
        // Phone number: 138****8000
        if (PHONE_PATTERN.matcher(trimmed).matches()) {
            return maskPhone(trimmed);
        }
        
        // ID card: 110101********1234
        if (ID_CARD_PATTERN.matcher(trimmed).matches()) {
            return maskIdCard(trimmed);
        }
        
        // Email: abc***@example.com
        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            return maskEmail(trimmed);
        }
        
        // Bank card: **** **** **** 1234
        if (BANK_CARD_PATTERN.matcher(trimmed).matches()) {
            return maskBankCard(trimmed);
        }
        
        // Default: mask middle part
        return maskDefault(trimmed);
    }
    
    /**
     * Mask phone number: keep first 3 and last 4 digits
     */
    private String maskPhone(String phone) {
        if (phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    /**
     * Mask ID card: keep first 6 and last 4 digits
     */
    private String maskIdCard(String idCard) {
        if (idCard.length() < 10) {
            return "****";
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
    
    /**
     * Mask email: mask username part
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***@***";
        }
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (username.length() <= 2) {
            return "***" + domain;
        }
        return username.substring(0, 2) + "***" + domain;
    }
    
    /**
     * Mask bank card: keep last 4 digits
     */
    private String maskBankCard(String card) {
        if (card.length() < 4) {
            return "****";
        }
        return "**** **** **** " + card.substring(card.length() - 4);
    }
    
    /**
     * Default masking: mask middle 60% of the string
     */
    private String maskDefault(String value) {
        if (value.length() <= 4) {
            return "****";
        }
        int maskStart = value.length() / 5;
        int maskEnd = value.length() - value.length() / 5;
        return value.substring(0, maskStart) + 
               "*".repeat(maskEnd - maskStart) + 
               value.substring(maskEnd);
    }
}

