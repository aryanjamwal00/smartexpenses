package com.aryan.smartexpense.controller;

import com.aryan.smartexpense.parser.CsvStatementParserService;
import com.aryan.smartexpense.parser.PdfStatementParserService;
import com.aryan.smartexpense.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final PdfStatementParserService pdfParserService;
    private final CsvStatementParserService csvParserService;

    // Inject all services via constructor
    public ExpenseController(ExpenseService expenseService,
                             PdfStatementParserService pdfParserService,
                             CsvStatementParserService csvParserService) {
        this.expenseService = expenseService;
        this.pdfParserService = pdfParserService;
        this.csvParserService = csvParserService;
    }

    @PostMapping(value = "/upload-pdf", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPdfStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("PDF file cannot be empty.");
        }

        try {
            // Call via the injected instance variable, NOT the class name
            var rows = pdfParserService.parse(file, password);
            return ResponseEntity.ok(expenseService.processCsvBatch(rows));
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            return ResponseEntity.status(401).body("Invalid or missing PDF password.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to parse statement: " + e.getMessage());
        }
    }

    @PostMapping(value = "/upload-csv", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCsvStatement(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("CSV file cannot be empty.");
        }

        try {
            var rows = csvParserService.parse(file);
            return ResponseEntity.ok(expenseService.processCsvBatch(rows));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to parse CSV: " + e.getMessage());
        }
    }

    @PostMapping("/parse-sms")
    public ResponseEntity<?> ingestSms(@RequestBody Map<String, String> payload) {
        String sms = payload.get("message");
        if (sms == null || sms.isBlank()) {
            return ResponseEntity.badRequest().body("SMS body cannot be empty");
        }

        try {
            return ResponseEntity.ok(expenseService.processSms(sms));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @GetMapping("/report")
    public ResponseEntity<?> getReport(@RequestParam("month") int month, @RequestParam("year") int year) {
        return ResponseEntity.ok(expenseService.getMonthlyReport(month, year));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(expenseService.getHistoricalReports());
    }

    @DeleteMapping("/reset")
    public ResponseEntity<?> resetCurrentMonth(@RequestParam("month") int month, @RequestParam("year") int year) {
        expenseService.resetMonthlyExpenses(month, year);
        return ResponseEntity.ok(Map.of("message", "Monthly expenses reset successfully."));
    }

    @PostMapping("/merchant-rule")
    public ResponseEntity<?> addMerchantRule(@RequestBody Map<String, String> payload) {
        String keyword = payload.get("keyword");
        String category = payload.get("category");

        if (keyword == null || keyword.isBlank() || category == null || category.isBlank()) {
            return ResponseEntity.badRequest().body("Keyword and category are required.");
        }

        try {
            var rule = expenseService.addRule(keyword, category);
            return ResponseEntity.ok(Map.of(
                "message", "Merchant rule added successfully",
                "keyword", rule.getKeyword(),
                "category", rule.getCategory().getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add rule: " + e.getMessage());
        }
    }

    @PostMapping("/manual-entry")
    public ResponseEntity<?> addManualEntry(@RequestBody Map<String, Object> payload) {
        try {
            String merchant = (String) payload.get("merchant");
            String amountStr = (String) payload.get("amount");
            String typeStr = (String) payload.get("type");
            String category = (String) payload.get("category");

            if (merchant == null || merchant.isBlank()) {
                return ResponseEntity.badRequest().body("Merchant name is required.");
            }
            if (amountStr == null || amountStr.isBlank()) {
                return ResponseEntity.badRequest().body("Amount is required.");
            }
            if (typeStr == null || typeStr.isBlank()) {
                return ResponseEntity.badRequest().body("Transaction type is required.");
            }

            java.math.BigDecimal amount = new java.math.BigDecimal(amountStr);
            com.aryan.smartexpense.model.TransactionType type =
                com.aryan.smartexpense.model.TransactionType.valueOf(typeStr.toUpperCase());

            var result = expenseService.addManualEntry(merchant, amount, type, category);
            return ResponseEntity.ok(result);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid amount format.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid transaction type. Use DEBIT or CREDIT.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add entry: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            var userDetails = (com.aryan.smartexpense.security.CustomUserDetails)
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return ResponseEntity.ok(Map.of(
                "email", userDetails.getUsername(),
                "userId", userDetails.getUserId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(expenseService.getAllCategories());
    }
}