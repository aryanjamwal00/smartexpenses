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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(length = 500)
    private String rawMessage;

    public Transaction() {}

    public Transaction(String txnHash, BigDecimal amount, TransactionType type,
                       String merchant, Category category, User user, LocalDateTime transactionDate, String rawMessage) {
        this.txnHash = txnHash;
        this.amount = amount;
        this.type = type;
        this.merchant = merchant;
        this.category = category;
        this.user = user;
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
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    public TransactionType getType() {
        return type;
    }
    public void setType(TransactionType type) {
        this.type = type;
    }
    public String getMerchant() {
        return merchant;
    }
    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }
    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }
    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
    public String getRawMessage() {
        return rawMessage;
    }
    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }
}
