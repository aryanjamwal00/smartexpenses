package com.aryan.smartexpense.repository;

import com.aryan.smartexpense.model.MerchantRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRuleRepository extends JpaRepository<MerchantRule, Long> {
    Optional<MerchantRule> findByKeywordIgnoreCase(String keyword);
}
