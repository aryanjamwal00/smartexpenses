package com.aryan.smartexpense.service;

import com.aryan.smartexpense.parser.CsvStatementParserService;
import com.aryan.smartexpense.dto.ExpenseResponseDto;
import com.aryan.smartexpense.model.Category;
import com.aryan.smartexpense.model.MerchantRule;
import com.aryan.smartexpense.model.Transaction;
import com.aryan.smartexpense.parser.BankSmsParserService;
import com.aryan.smartexpense.repository.CategoryRepository;
import com.aryan.smartexpense.repository.MerchantRuleRepository;
import com.aryan.smartexpense.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExpenseService {

    @Transactional
    public Map<String, Object> processCsvBatch(List<CsvStatementParserService.CsvTransactionRow> rows) {
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

    public ExpenseService(TransactionRepository transactionRepo,
                          CategoryRepository categoryRepo,
                          MerchantRuleRepository ruleRepo,
                          BankSmsParserService parserService) {
        this.transactionRepo = transactionRepo;
        this.categoryRepo = categoryRepo;
        this.ruleRepo = ruleRepo;
        this.parserService = parserService;
    }

    @Transactional
    public ExpenseResponseDto processSms(String rawSms) {
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
                now,
                rawSms
        );

        Transaction savedTx = transactionRepo.save(transaction);

        // Budget check logic
        BigDecimal totalSpent = transactionRepo.getTotalSpentByCategoryAndMonth(
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
        List<Object[]> results = transactionRepo.getMonthlyExpensesByCategory(month, year);
        Map<String, Object> report = new HashMap<>();

        Map<String, Object> categoryBreakdown = new HashMap<>();
        for (Object[] row : results) {
            String category = (String) row[0];
            Object total = row[1];
            categoryBreakdown.put(category, total);
        }

        report.put("month", month);
        report.put("year", year);
        report.put("breakdown", categoryBreakdown);
        return report;
    }

    public List<Map<String, Object>> getHistoricalReports() {
        List<Object[]> records = transactionRepo.getHistoricalMonthlySummaries();
        List<Map<String, Object>> history = new ArrayList<>();

        for (Object[] row : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("year", row[0]);
            item.put("month", row[1]);
            item.put("total", row[2]);
            history.add(item);
        }
        return history;
    }

    @Transactional
    public void resetMonthlyExpenses(int month, int year) {
        transactionRepo.deleteExpensesByMonthAndYear(month, year);
    }
}