package com.aryan.smartexpense.parser;

import com.aryan.smartexpense.model.TransactionType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfStatementParserService {

    // Matches both "01 Sep 2026" and "01/09/2026"
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(\\d{2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}|\\d{2}[/-]\\d{2}[/-]\\d{4})\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Matches currency amounts like 45.00, 4,365.00, 12,500.00
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "\\b([0-9]+(?:,[0-9]{2,3})*\\.\\d{2})\\b"
    );
    private static final Pattern CREDIT_PATTERN = Pattern.compile(
            "\\b(?:CR|CREDIT|CREDITED|RECEIVED|DEPOSITED|REFUND(?:ED)?|CASHBACK|" +
                    "TRANSFER(?:RED)? FROM|UPI FROM|NEFT\\s*CR|IMPS\\s*CR|RTGS\\s*CR)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DEBIT_PATTERN = Pattern.compile(
            "\\b(?:DR|DEBIT|DEBITED|PAID|PURCHASE|WITHDRAWN?|SENT TO|TRANSFER(?:RED)? TO|" +
                    "UPI TO|NEFT\\s*DR|IMPS\\s*DR|RTGS\\s*DR)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ACTIVITY_PATTERN = Pattern.compile(
            "\\b(?:UPI|NEFT|IMPS|RTGS|TRANSFER(?:RED)?|RECEIVED|CREDITED|DEBITED|DEPOSITED|" +
                    "REFUND(?:ED)?|CASHBACK|PURCHASE|PAID|WITHDRAWN?|SENT TO)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public List<CsvStatementParserService.CsvTransactionRow> parse(MultipartFile file, String password) throws IOException {
        List<CsvStatementParserService.CsvTransactionRow> rows = new ArrayList<>();
        byte[] fileBytes = file.getBytes();

        try (PDDocument document = (password != null && !password.isBlank())
                ? Loader.loadPDF(fileBytes, password)
                : Loader.loadPDF(fileBytes)) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // Preserves table layout coordinates
            String fullText = stripper.getText(document);

            String[] lines = fullText.split("\\r?\\n");
            String currentDate = null;
            BigDecimal previousBalance = null;

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || line.toLowerCase().contains("account statement")) {
                    continue;
                }

                // Check if line starts or contains a transaction date
                Matcher dateMatcher = DATE_PATTERN.matcher(line);
                if (dateMatcher.find()) {
                    currentDate = dateMatcher.group(1);
                }

                Matcher amountMatcher = AMOUNT_PATTERN.matcher(line);
                List<String> amounts = new ArrayList<>();
                while (amountMatcher.find()) {
                    amounts.add(amountMatcher.group(1));
                }

                // Use the opening balance as a baseline when a statement omits explicit Dr/Cr labels.
                if (line.toLowerCase().contains("opening balance") && !amounts.isEmpty()) {
                    previousBalance = parseAmount(amounts.get(amounts.size() - 1));
                    continue;
                }

                if (line.toLowerCase().contains("balance")
                        && !ACTIVITY_PATTERN.matcher(line).find()) {
                    continue;
                }

                // Kotak format: usually [Transaction Amount, Running Balance]
                if (!amounts.isEmpty() && currentDate != null) {
                    // The last currency value is the running balance; preceding value is the transaction amount.
                    if (amounts.size() < 2) continue;
                    BigDecimal txAmount = parseAmount(amounts.get(amounts.size() - 2));
                    BigDecimal currentBalance = parseAmount(amounts.get(amounts.size() - 1));

                    if (txAmount.compareTo(BigDecimal.ZERO) > 0) {
                        // Prefer explicit indicators, then infer direction from the running balance change.
                        boolean credit = CREDIT_PATTERN.matcher(line).find();
                        boolean debit = DEBIT_PATTERN.matcher(line).find();
                        TransactionType type;
                        if (credit) {
                            type = TransactionType.CREDIT;
                        } else if (debit) {
                            type = TransactionType.DEBIT;
                        } else if (previousBalance != null && currentBalance.compareTo(previousBalance) != 0) {
                            type = currentBalance.compareTo(previousBalance) > 0
                                    ? TransactionType.CREDIT : TransactionType.DEBIT;
                        } else {
                            previousBalance = currentBalance;
                            continue;
                        }

                        String desc = line.replaceAll("[0-9,]+\\.\\d{2}", "").replaceAll(DATE_PATTERN.pattern(), "").trim();
                        if (desc.isEmpty()) desc = "BANK_TXN";

                        String hash = generateSha256(currentDate + "_" + desc + "_" + txAmount + "_" + type);
                        rows.add(new CsvStatementParserService.CsvTransactionRow(desc, txAmount, type, hash));
                    }
                    previousBalance = currentBalance;
                }
            }
        }
        return rows;
    }

    private BigDecimal parseAmount(String value) {
        return new BigDecimal(value.replace(",", ""));
    }

    private String generateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
