package com.aryan.smartexpense.dto;

import com.aryan.smartexpense.model.Transaction;
import java.math.BigDecimal;

public class ExpenseResponseDto {
    private Transaction transaction;
    private boolean budgetExceeded;
    private BigDecimal monthlyBudget;
    private BigDecimal totalSpentThisMonth;
    private String alertMessage;

    public ExpenseResponseDto(Transaction transaction, boolean budgetExceeded,
                              BigDecimal monthlyBudget, BigDecimal totalSpentThisMonth,
                              String alertMessage) {
        this.transaction = transaction;
        this.budgetExceeded = budgetExceeded;
        this.monthlyBudget = monthlyBudget;
        this.totalSpentThisMonth = totalSpentThisMonth;
        this.alertMessage = alertMessage;
    }

    public Transaction getTransaction() {
        return transaction;
    }
    public boolean isBudgetExceeded() {
        return budgetExceeded;
    }
    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }
    public BigDecimal getTotalSpentThisMonth() {
        return totalSpentThisMonth;
    }
    public String getAlertMessage() {
        return alertMessage;
    }
}