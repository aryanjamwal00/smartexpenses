package com.aryan.smartexpense.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "Food & Dining", "Shopping", "Groceries", "Utilities"

    private BigDecimal monthlyBudget;

    public Category() {}

    public Category(String name, BigDecimal monthlyBudget) {
        this.name = name;
        this.monthlyBudget = monthlyBudget;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }
    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

}
