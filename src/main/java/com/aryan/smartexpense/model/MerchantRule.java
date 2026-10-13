package com.aryan.smartexpense.model;

import jakarta.persistence.*;

@Entity
@Table(name ="merchant_rules")
public class MerchantRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "SWIGGY", "ZEPTO", "NETFLIX"
    @Column(nullable = false, unique = true)
    private String keyword;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public MerchantRule() {}

    public MerchantRule(String keyword, Category category) {
        this.keyword = keyword.toUpperCase();
        this.category = category;
    }

    public Long getId() {
        return id;
    }
    public String getKeyword() {
        return keyword;
    }
    public void setKeyword(String keyword) {
        this.keyword = keyword.toUpperCase();
    }
    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }
}
