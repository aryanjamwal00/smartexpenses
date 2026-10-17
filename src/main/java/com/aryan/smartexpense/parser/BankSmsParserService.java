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
            "(?:(?:₹|RS\\.?|INR\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?))",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern OTP_PATTERN = Pattern.compile(
            "\\b(?:otp|verification|verify|one[ -]?time|onetime|2fa|mfa|pin|password|auth|authenticate|" +
                    "login|sign in|security|secure|expiry|expire|validity|valid for|confirm|code)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CREDIT_PATTERN = Pattern.compile(
            "\\b(?:credited|received|deposited|refund(?:ed)?|cashback|reversed|" +
                    "transfer(?:red)? from|received from|upi from|neft\\s*cr|imps\\s*cr|rtgs\\s*cr)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DEBIT_PATTERN = Pattern.compile(
            "\\b(?:debited|debit|spent|paid|purchase|withdrawn?|sent to|transfer(?:red)? to|" +
                    "upi to|neft\\s*dr|imps\\s*dr|rtgs\\s*dr)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Regex to capture merchant names - improved patterns for various SMS formats
    private static final Pattern MERCHANT_PATTERN = Pattern.compile(
            "(?:to|at|vpa|@|with)\\s*([a-zA-Z0-9@._\\s-]+?)(?:\\s*(?:on|for|via|using|at|rs|inr|\\d|\\(|$))",
            Pattern.CASE_INSENSITIVE
    );

    // Additional pattern for UPI handles and merchant names
    private static final Pattern UPI_PATTERN = Pattern.compile(
            "([a-zA-Z0-9.-]+@[a-zA-Z0-9.-]+)",
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
        if (smsText == null || smsText.isBlank()) {
            throw new IllegalArgumentException("SMS is empty");
        }
        String cleanSms = smsText.trim();

        if (OTP_PATTERN.matcher(cleanSms).find()) {
            throw new IllegalArgumentException("OTP and verification messages are not accepted");
        }

        // 1. Extract Amount
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(cleanSms);
        BigDecimal amount = BigDecimal.ZERO;
        if (amountMatcher.find()) {
            String rawAmount = amountMatcher.group(1).replace(",", "");
            amount = new BigDecimal(rawAmount);
        }

        // 2. Identify Type (Debit vs Credit)
        boolean credit = CREDIT_PATTERN.matcher(cleanSms).find();
        boolean debit = DEBIT_PATTERN.matcher(cleanSms).find();
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || (!credit && !debit)) {
            throw new IllegalArgumentException("Only SMS messages for debit or credit transactions are accepted");
        }
        // Incoming funds, refunds, and cashback always belong in CREDIT.
        TransactionType type = credit ? TransactionType.CREDIT : TransactionType.DEBIT;

        // 3. Extract Merchant Name
        String merchant = "UNKNOWN";

        // Try UPI pattern first (e.g., swiggy@ybl, amazon@axisbank)
        Matcher upiMatcher = UPI_PATTERN.matcher(cleanSms);
        if (upiMatcher.find()) {
            String upiHandle = upiMatcher.group(1).toUpperCase(Locale.ROOT);
            // Extract merchant name before @
            merchant = upiHandle.split("@")[0].replace(".", " ").trim();
        } else {
            // Try merchant pattern (to/at/with merchant)
            Matcher merchantMatcher = MERCHANT_PATTERN.matcher(cleanSms);
            if (merchantMatcher.find()) {
                merchant = merchantMatcher.group(1).toUpperCase(Locale.ROOT).trim();
            }
        }

        // Clean up merchant name
        merchant = merchant.replaceAll("[@._-]", " ").replaceAll("\\s+", " ").trim();

        // If still unknown, try to find common merchant keywords in the SMS
        if (merchant.equals("UNKNOWN")) {
            String[] commonMerchants = {
                "SWIGGY", "ZOMATO", "BLINKIT", "ZEPTO", "AMAZON", "FLIPKART",
                "UBER", "OLA", "RAPIDO", "NETFLIX", "SPOTIFY", "PAYTM", "PHONEPE",
                "GPAY", "BIGBASKET", "DUNZO", "MYNTRA", "AJIO", "NYKAA"
            };
            for (String m : commonMerchants) {
                if (cleanSms.toUpperCase(Locale.ROOT).contains(m)) {
                    merchant = m;
                    break;
                }
            }
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
