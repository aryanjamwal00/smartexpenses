package com.aryan.smartexpense.repository;

import com.aryan.smartexpense.model.Category;
import com.aryan.smartexpense.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTxnHash(String txnHash);

    // Custom query to aggregate monthly expense by category
    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t " +
            "WHERE t.type = 'DEBIT' AND MONTH(t.transactionDate) = :month AND YEAR(t.transactionDate) = :year " +
            "GROUP BY t.category.name")
    List<Object[]> getMonthlyExpensesByCategory(@Param("month") int month, @Param("year") int year);

    // Sum expenses for a specific category in a given month and year
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.category = :category AND t.type = 'DEBIT' " +
            "AND MONTH(t.transactionDate) = :month AND YEAR(t.transactionDate) = :year")
    BigDecimal getTotalSpentByCategoryAndMonth(
            @Param("category") Category category,
            @Param("month") int month,
            @Param("year") int year
    );

    // Fetch all distinct months and years with their total spends
    @Query("SELECT YEAR(t.transactionDate), MONTH(t.transactionDate), SUM(t.amount) " +
            "FROM Transaction t WHERE t.type = 'DEBIT' " +
            "GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate) " +
            "ORDER BY YEAR(t.transactionDate) DESC, MONTH(t.transactionDate) DESC")
    List<Object[]> getHistoricalMonthlySummaries();

    // Reset/Delete transactions for a given month and year
    @Modifying
    @Query("DELETE FROM Transaction t WHERE MONTH(t.transactionDate) = :month AND YEAR(t.transactionDate) = :year")
    void deleteExpensesByMonthAndYear(@Param("month") int month, @Param("year") int year);
}
