package com.aryan.smartexpense.parser;

import com.aryan.smartexpense.model.TransactionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvStatementParserService {

    public static class CsvTransactionRow {
        public String description;
        public BigDecimal amount;
        public TransactionType type;
        public String hash;

        public CsvTransactionRow(String description, BigDecimal amount, TransactionType type, String hash) {
            this.description = description;
            this.amount = amount;
            this.type = type;
            this.hash = hash;
        }
    }

    // Expects CSV columns: Date, Description, Debit, Credit
    public List<CsvTransactionRow> parse(MultipartFile file) throws Exception {
        List<CsvTransactionRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord record : csvParser) {
                String desc = record.isMapped("Description") ? record.get("Description") : "UNKNOWN";
                String debitStr = record.isMapped("Debit") ? record.get("Debit") : "";
                String creditStr = record.isMapped("Credit") ? record.get("Credit") : "";

                BigDecimal amount = BigDecimal.ZERO;
                TransactionType type = TransactionType.DEBIT;

                if (!debitStr.isBlank() && !debitStr.equals("0")) {
                    amount = new BigDecimal(debitStr.replace(",", ""));
                    type = TransactionType.DEBIT;
                } else if (!creditStr.isBlank() && !creditStr.equals("0")) {
                    amount = new BigDecimal(creditStr.replace(",", ""));
                    type = TransactionType.CREDIT;
                } else {
                    continue; // Skip zero/empty rows
                }

                String rawIdentity = desc + "_" + amount + "_" + type;
                String hash = generateSha256(rawIdentity);

                rows.add(new CsvTransactionRow(desc, amount, type, hash));
            }
        }

        return rows;
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
        } catch (Exception e) {
            throw new RuntimeException("Error computing hash", e);
        }
    }
}