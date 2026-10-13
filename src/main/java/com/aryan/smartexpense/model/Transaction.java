package com.aryan.smartexpense.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique hash to prevent duplicate entries from multiple imports/SMS
    @Column(nullable = false, unique = true, length = 64)
    private String txnHash;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    private String merchant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(length = 500)
    private String rawMessage;

    public Transaction() {}

    public Transaction(String txnHash, BigDecimal amount, TransactionType type,
                       String merchant, Category category, LocalDateTime transactionDate, String rawMessage) {
        this.txnHash = txnHash;
        this.amount = amount;
        this.type = type;
        this.merchant = merchant;
        this.category = category;
        this.transactionDate = transactionDate;
        this.rawMessage = rawMessage;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }
    public String getTxnHash() {
        return txnHash;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public TransactionType getType() {
        return type;
    }
    public String getMerchant() {
        return merchant;
    }
    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }
    public String getRawMessage() {
        return rawMessage;
    }
}
