package com.aryan.smartexpense.controller;

import com.aryan.smartexpense.parser.CsvStatementParserService;
import com.aryan.smartexpense.parser.PdfStatementParserService;
import com.aryan.smartexpense.service.ExpenseService;
import com.aryan.smartexpense.repository.UserRepository;
import com.aryan.smartexpense.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final PdfStatementParserService pdfParserService;
    private final CsvStatementParserService csvParserService;
    private final UserRepository userRepo;

    // Inject all services via constructor
    public ExpenseController(ExpenseService expenseService,
                             PdfStatementParserService pdfParserService,
                             CsvStatementParserService csvParserService,
                             UserRepository userRepo) {
        this.expenseService = expenseService;
        this.pdfParserService = pdfParserService;
        this.csvParserService = csvParserService;
        this.userRepo = userRepo;
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

    // Webhook endpoint for SMS forwarding services (Twilio, Plivo, etc.)
    @PostMapping("/sms-webhook")
    public ResponseEntity<?> handleSmsWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String webhookSecret,
            @RequestBody Map<String, Object> payload) {
        try {
            // Verify webhook secret (optional but recommended for security)
            String expectedSecret = System.getenv("WEBHOOK_SECRET");
            if (expectedSecret != null && !expectedSecret.isEmpty() && !expectedSecret.equals(webhookSecret)) {
                return ResponseEntity.status(401).body("Invalid webhook secret");
            }

            // Support multiple SMS forwarding service formats
            String smsText = extractSmsText(payload);
            String sender = extractSender(payload);

            if (smsText == null || smsText.isBlank()) {
                return ResponseEntity.badRequest().body("No SMS text found in payload");
            }

            // Find user by phone number (if provided in payload or use sender)
            String phoneNumber = extractPhoneNumber(payload);
            if (phoneNumber == null || phoneNumber.isBlank()) {
                phoneNumber = sender; // Use sender as phone number
            }

            // Clean phone number (remove +, spaces, dashes)
            if (phoneNumber != null) {
                phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
            }

            User user = null;
            if (phoneNumber != null && !phoneNumber.isBlank()) {
                Optional<User> userOpt = userRepo.findByPhoneNumber(phoneNumber);
                if (userOpt.isPresent()) {
                    user = userOpt.get();
                }
            }

            // If no user found, return error
            if (user == null) {
                return ResponseEntity.status(404).body("No user found for phone number: " + phoneNumber);
            }

            // Process the SMS with user context
            var result = expenseService.processSmsForUser(smsText, user);

            return ResponseEntity.ok(Map.of(
                "status", "processed",
                "message", "SMS processed successfully",
                "data", result
            ));
        } catch (IllegalArgumentException e) {
            // Duplicate transaction - return success to prevent webhook retries
            return ResponseEntity.ok(Map.of(
                "status", "duplicate",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to process SMS: " + e.getMessage());
        }
    }

    private String extractPhoneNumber(Map<String, Object> payload) {
        // Try common phone number field names
        String[] fields = {"phone", "phoneNumber", "phone_number", "To", "to", "recipient"};
        for (String field : fields) {
            if (payload.containsKey(field) && payload.get(field) != null) {
                return payload.get(field).toString();
            }
        }
        return null;
    }

    private String extractSmsText(Map<String, Object> payload) {
        // Try common field names from different SMS services
        String[] fields = {"Body", "body", "text", "message", "content", "sms", "Message"};
        for (String field : fields) {
            if (payload.containsKey(field) && payload.get(field) != null) {
                return payload.get(field).toString();
            }
        }
        return null;
    }

    private String extractSender(Map<String, Object> payload) {
        // Try common sender field names
        String[] fields = {"From", "from", "sender", "source", "Sender"};
        for (String field : fields) {
            if (payload.containsKey(field) && payload.get(field) != null) {
                return payload.get(field).toString();
            }
        }
        return "Unknown";
    }
}