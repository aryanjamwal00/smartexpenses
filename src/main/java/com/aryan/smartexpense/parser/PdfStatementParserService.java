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
            "\\b([0-9]{1,3}(?:,[0-9]{2,3})*\\.\\d{2})\\b"
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

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || line.contains("Account Statement") || line.contains("Balance")) {
                    continue;
                }

                // Check if line starts or contains a transaction date
                Matcher dateMatcher = DATE_PATTERN.matcher(line);
                if (dateMatcher.find()) {
                    currentDate = dateMatcher.group(1);
                }

                // Find amounts on the line
                Matcher amountMatcher = AMOUNT_PATTERN.matcher(line);
                List<String> amounts = new ArrayList<>();
                while (amountMatcher.find()) {
                    amounts.add(amountMatcher.group(1));
                }

                // Kotak format: usually [Transaction Amount, Running Balance]
                if (!amounts.isEmpty() && currentDate != null) {
                    BigDecimal txAmount;
                    if (amounts.size() >= 2) {
                        txAmount = new BigDecimal(amounts.get(amounts.size() - 2).replace(",", ""));
                    } else {
                        txAmount = new BigDecimal(amounts.get(0).replace(",", ""));
                    }

                    if (txAmount.compareTo(BigDecimal.ZERO) > 0) {
                        // Classify Credit vs Debit based on common indicators
                        TransactionType type = (line.toUpperCase().contains("CR")
                                || line.toUpperCase().contains("CASHBACK")
                                || line.toUpperCase().contains("REFUND")
                                || line.toUpperCase().contains("INT. PD"))
                                ? TransactionType.CREDIT
                                : TransactionType.DEBIT;

                        String desc = line.replaceAll("[0-9,]+\\.\\d{2}", "").replaceAll(DATE_PATTERN.pattern(), "").trim();
                        if (desc.isEmpty()) desc = "BANK_TXN";

                        String hash = generateSha256(currentDate + "_" + desc + "_" + txAmount + "_" + type);
                        rows.add(new CsvStatementParserService.CsvTransactionRow(desc, txAmount, type, hash));
                    }
                }
            }
        }
        return rows;
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