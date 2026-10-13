package com.aryan.smartexpense.parser;

import com.aryan.smartexpense.model.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BankSmsParserService {

    // Regex to capture amounts like Rs. 450, INR 1,299.50, Rs 500.00
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:(?:RS|INR|Rs\\.|INR\\.)\\s*([0-9,]+(?:\\.[0-9]{1,2})?))",
            Pattern.CASE_INSENSITIVE
    );

    // Regex to capture common merchant keywords following 'to' or 'at'
    private static final Pattern MERCHANT_PATTERN = Pattern.compile(
            "(?:to|at|vpa)\\s+([a-zA-Z0-9@._-]+)",
            Pattern.CASE_INSENSITIVE
    );

    public static class ParsedData {
        public BigDecimal amount;
        public TransactionType type;
        public String merchant;
        public String hash;

        public ParsedData(BigDecimal amount, TransactionType type, String merchant, String hash) {
            this.amount = amount;
            this.type = type;
            this.merchant = merchant;
            this.hash = hash;
        }
    }

    public ParsedData parse(String smsText) {
        String cleanSms = smsText.trim();

        // 1. Extract Amount
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(cleanSms);
        BigDecimal amount = BigDecimal.ZERO;
        if (amountMatcher.find()) {
            String rawAmount = amountMatcher.group(1).replace(",", "");
            amount = new BigDecimal(rawAmount);
        }

        // 2. Identify Type (Debit vs Credit)
        TransactionType type = TransactionType.DEBIT;
        String lower = cleanSms.toLowerCase(Locale.ROOT);
        if (lower.contains("credited") || lower.contains("deposited") || lower.contains("received")) {
            type = TransactionType.CREDIT;
        }

        // 3. Extract Merchant Name
        String merchant = "UNKNOWN";
        Matcher merchantMatcher = MERCHANT_PATTERN.matcher(cleanSms);
        if (merchantMatcher.find()) {
            merchant = merchantMatcher.group(1).toUpperCase(Locale.ROOT);
        }

        // 4. Generate Idempotency/Deduplication Hash (SHA-256)
        String hash = generateSha256(cleanSms);

        return new ParsedData(amount, type, merchant, hash);
    }

    private String generateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

}
