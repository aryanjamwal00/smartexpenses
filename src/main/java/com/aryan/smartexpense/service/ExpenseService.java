package com.aryan.smartexpense.service;

import com.aryan.smartexpense.parser.CsvStatementParserService;
import com.aryan.smartexpense.dto.ExpenseResponseDto;
import com.aryan.smartexpense.model.Category;
import com.aryan.smartexpense.model.MerchantRule;
import com.aryan.smartexpense.model.Transaction;
import com.aryan.smartexpense.model.TransactionType;
import com.aryan.smartexpense.model.User;
import com.aryan.smartexpense.parser.BankSmsParserService;
import com.aryan.smartexpense.repository.CategoryRepository;
import com.aryan.smartexpense.repository.MerchantRuleRepository;
import com.aryan.smartexpense.repository.TransactionRepository;
import com.aryan.smartexpense.repository.UserRepository;
import com.aryan.smartexpense.security.CustomUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExpenseService {

    @Transactional
    public Map<String, Object> processCsvBatch(List<CsvStatementParserService.CsvTransactionRow> rows) {
        User currentUser = getCurrentUser();
        int inserted = 0;
        int duplicates = 0;

        for (CsvStatementParserService.CsvTransactionRow row : rows) {
            if (transactionRepo.findByTxnHash(row.hash).isPresent()) {
                duplicates++;
                continue;
            }

            Category category = resolveCategoryForMerchant(row.description);
            Transaction tx = new Transaction(
                    row.hash,
                    row.amount,
                    row.type,
                    row.description,
                    category,
                    currentUser,
                    LocalDateTime.now(),
                    "STATEMENT_IMPORT: " + row.description
            );

            transactionRepo.save(tx);
            inserted++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("inserted", inserted);
        result.put("duplicatesSkipped", duplicates);
        return result;
    }

    private final TransactionRepository transactionRepo;
    private final CategoryRepository categoryRepo;
    private final MerchantRuleRepository ruleRepo;
    private final BankSmsParserService parserService;
    private final UserRepository userRepo;

    public ExpenseService(TransactionRepository transactionRepo,
                          CategoryRepository categoryRepo,
                          MerchantRuleRepository ruleRepo,
                          BankSmsParserService parserService,
                          UserRepository userRepo) {
        this.transactionRepo = transactionRepo;
        this.categoryRepo = categoryRepo;
        this.ruleRepo = ruleRepo;
        this.parserService = parserService;
        this.userRepo = userRepo;
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepo.findById(userDetails.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public ExpenseResponseDto processSms(String rawSms) {
        User currentUser = getCurrentUser();
        return processSmsForUser(rawSms, currentUser);
    }

    @Transactional
    public ExpenseResponseDto processSmsForUser(String rawSms, User user) {
        BankSmsParserService.ParsedData data = parserService.parse(rawSms);

        if (transactionRepo.findByTxnHash(data.hash).isPresent()) {
            throw new IllegalArgumentException("Duplicate transaction detected. Skipping.");
        }

        Category assignedCategory = resolveCategoryForMerchant(data.merchant);
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction(
                data.hash,
                data.amount,
                data.type,
                data.merchant,
                assignedCategory,
                user,
                now,
                rawSms
        );

        Transaction savedTx = transactionRepo.save(transaction);

        // Budget check logic
        BigDecimal totalSpent = transactionRepo.getTotalSpentByCategoryAndMonth(
                user,
                assignedCategory,
                now.getMonthValue(),
                now.getYear()
        );

        BigDecimal budget = assignedCategory.getMonthlyBudget();
        boolean exceeded = false;
        String alert = "Within budget limits.";

        if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
            if (totalSpent.compareTo(budget) > 0) {
                exceeded = true;
                BigDecimal overBy = totalSpent.subtract(budget);
                alert = String.format("ALERT: You have exceeded the monthly budget for '%s' by Rs. %s!",
                        assignedCategory.getName(), overBy);
            } else if (totalSpent.compareTo(budget.multiply(new BigDecimal("0.85"))) >= 0) {
                alert = String.format("WARNING: You have used over 85%% of your '%s' budget.",
                        assignedCategory.getName());
            }
        }

        return new ExpenseResponseDto(savedTx, exceeded, budget, totalSpent, alert);
    }

    @Transactional
    public Category setCategoryBudget(String categoryName, BigDecimal budget) {
        Category category = categoryRepo.findByNameIgnoreCase(categoryName)
                .orElseGet(() -> new Category(categoryName, budget));
        category.setMonthlyBudget(budget);
        return categoryRepo.save(category);
    }

    private Category resolveCategoryForMerchant(String merchant) {
        List<MerchantRule> allRules = ruleRepo.findAll();
        for (MerchantRule rule : allRules) {
            if (merchant.toUpperCase().contains(rule.getKeyword())) {
                return rule.getCategory();
            }
        }

        return categoryRepo.findByNameIgnoreCase("Miscellaneous")
                .orElseGet(() -> categoryRepo.save(new Category("Miscellaneous", BigDecimal.ZERO)));
    }

    @Transactional
    public MerchantRule addRule(String keyword, String categoryName) {
        Category category = categoryRepo.findByNameIgnoreCase(categoryName)
                .orElseGet(() -> categoryRepo.save(new Category(categoryName, BigDecimal.ZERO)));

        MerchantRule rule = ruleRepo.findByKeywordIgnoreCase(keyword)
                .orElse(new MerchantRule(keyword, category));

        rule.setCategory(category);
        return ruleRepo.save(rule);
    }

    public Map<String, Object> getMonthlyReport(int month, int year) {
        User currentUser = getCurrentUser();
        List<Object[]> categoryResults = transactionRepo.getMonthlyExpensesByCategory(currentUser, month, year);
        List<Object[]> merchantResults = transactionRepo.getMonthlyExpensesByMerchant(currentUser, month, year);
        BigDecimal totalRefunds = transactionRepo.getTotalRefundsForMonth(currentUser, month, year);
        List<Object[]> refundDetails = transactionRepo.getRefundDetailsForMonth(currentUser, month, year);

        Map<String, Object> report = new HashMap<>();

        // Category breakdown
        Map<String, Object> categoryBreakdown = new HashMap<>();
        for (Object[] row : categoryResults) {
            String category = (String) row[0];
            Object total = row[1];
            categoryBreakdown.put(category, total);
        }

        // Merchant-wise breakdown
        List<Map<String, Object>> merchantBreakdown = new ArrayList<>();
        for (Object[] row : merchantResults) {
            Map<String, Object> merchantInfo = new HashMap<>();
            merchantInfo.put("merchant", row[0]);
            merchantInfo.put("category", row[1]);
            merchantInfo.put("amount", row[2]);
            merchantBreakdown.add(merchantInfo);
        }

        // Refund details
        List<Map<String, Object>> refundList = new ArrayList<>();
        for (Object[] row : refundDetails) {
            Map<String, Object> refundInfo = new HashMap<>();
            refundInfo.put("merchant", row[0]);
            refundInfo.put("amount", row[1]);
            refundInfo.put("date", row[2]);
            refundList.add(refundInfo);
        }

        report.put("month", month);
        report.put("year", year);
        report.put("categoryBreakdown", categoryBreakdown);
        report.put("merchantBreakdown", merchantBreakdown);
        report.put("totalRefunds", totalRefunds);
        report.put("refundDetails", refundList);
        return report;
    }

    public List<Map<String, Object>> getHistoricalReports() {
        User currentUser = getCurrentUser();
        List<Object[]> records = transactionRepo.getHistoricalMonthlySummaries(currentUser);
        List<Map<String, Object>> history = new ArrayList<>();

        for (Object[] row : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("year", row[0]);
            item.put("month", row[1]);
            // Handle null or negative values
            BigDecimal total = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            item.put("total", total);
            history.add(item);
        }
        return history;
    }

    @Transactional
    public void resetMonthlyExpenses(int month, int year) {
        User currentUser = getCurrentUser();
        transactionRepo.deleteExpensesByMonthAndYear(currentUser, month, year);
    }

    @Transactional
    public Map<String, Object> addManualEntry(String merchant, BigDecimal amount, TransactionType type, String categoryName) {
        User currentUser = getCurrentUser();
        // Generate hash for deduplication
        String rawIdentity = merchant + "_" + amount + "_" + type + "_MANUAL_" + System.currentTimeMillis();
        String hash = generateSha256(rawIdentity);

        // Check for duplicates
        if (transactionRepo.findByTxnHash(hash).isPresent()) {
            throw new IllegalArgumentException("Duplicate transaction detected.");
        }

        // Resolve category
        Category category;
        if (categoryName != null && !categoryName.isBlank()) {
            category = categoryRepo.findByNameIgnoreCase(categoryName)
                    .orElseGet(() -> categoryRepo.save(new Category(categoryName, BigDecimal.ZERO)));
        } else {
            category = resolveCategoryForMerchant(merchant);
        }

        // Create transaction
        Transaction transaction = new Transaction(
                hash,
                amount,
                type,
                merchant,
                category,
                currentUser,
                LocalDateTime.now(),
                "MANUAL_ENTRY"
        );

        transactionRepo.save(transaction);

        return Map.of(
                "message", "Manual entry added successfully",
                "merchant", merchant,
                "amount", amount,
                "type", type.toString(),
                "category", category.getName()
        );
    }

    public List<Map<String, Object>> getAllCategories() {
        List<Category> categories = categoryRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category cat : categories) {
            Map<String, Object> catData = new HashMap<>();
            catData.put("id", cat.getId());
            catData.put("name", cat.getName());
            catData.put("monthlyBudget", cat.getMonthlyBudget());
            result.add(catData);
        }
        return result;
    }

    private String generateSha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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