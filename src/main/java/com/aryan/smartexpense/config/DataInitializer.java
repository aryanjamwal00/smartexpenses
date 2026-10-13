package com.aryan.smartexpense.config;

import com.aryan.smartexpense.model.Category;
import com.aryan.smartexpense.model.MerchantRule;
import com.aryan.smartexpense.repository.CategoryRepository;
import com.aryan.smartexpense.repository.MerchantRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepo;
    private final MerchantRuleRepository ruleRepo;

    public DataInitializer(CategoryRepository categoryRepo, MerchantRuleRepository ruleRepo) {
        this.categoryRepo = categoryRepo;
        this.ruleRepo = ruleRepo;
    }

    @Override
    public void run(String... args) {
        // Only initialize if no categories exist
        if (categoryRepo.count() == 0) {
            initializeCategoriesAndRules();
        }
    }

    private void initializeCategoriesAndRules() {
        // Define categories with their merchants
        Map<String, String[]> categoryMerchants = new HashMap<>();

        categoryMerchants.put("Food & Dining", new String[]{
            "SWIGGY", "ZOMATO", "DOMINOS", "PIZZA HUT", "KFC", "MCDONALDS",
            "BURGER KING", "SUBWAY", "HALDIRAMS", "FAASOS", "FOODPANDA"
        });

        categoryMerchants.put("Groceries", new String[]{
            "BLINKIT", "ZEPTO", "BIG BASKET", "GROFERS", "AMAZON FRESH",
            "DUNZO", "SPENCER'S", "RELIANCE FRESH", "DMART", "MORE"
        });

        categoryMerchants.put("Shopping", new String[]{
            "AMAZON", "FLIPKART", "MYNTRA", "AJIO", "NYKAA", "MEESHO",
            "TATA CLIQ", "CROMA", "RELIANCE DIGITAL", "SAMSUNG", "APPLE"
        });

        categoryMerchants.put("Transport", new String[]{
            "UBER", "OLA", "RAPIDO", "NAMMA YATRI", "BLU SMART",
            "MUMBAI METRO", "DELHI METRO", "IRCTC", "REDRAIL", "MAKEMYTRIP"
        });

        categoryMerchants.put("Entertainment", new String[]{
            "NETFLIX", "AMAZON PRIME", "DISNEY+", "HOTSTAR", "SPOTIFY",
            "YOUTUBE", "BOOKMYSHOW", "PAYTM MOVIES", "SONY LIV", "ZEE5"
        });

        categoryMerchants.put("Utilities", new String[]{
            "PAYTM", "PHONEPE", "GPAY", "AIRTEL", "JIO", "VI",
            "ELECTRICITY", "WATER", "GAS", "INTERNET", "BROADBAND"
        });

        categoryMerchants.put("Health & Wellness", new String[]{
            "PHARMACY", "1MG", "NETMEDS", "PHARMEASY", "PRACTO",
            "HEALTHKART", "GYM", "FITNESS", "YOGA", "MEDICAL"
        });

        categoryMerchants.put("Education", new String[]{
            "COURSERA", "UDEMY", "BYJU'S", "UNACADEMY", "VEDANTU",
            "SCHOOL", "COLLEGE", "UNIVERSITY", "FEES", "TUITION"
        });

        // Create categories and rules
        for (Map.Entry<String, String[]> entry : categoryMerchants.entrySet()) {
            String categoryName = entry.getKey();
            String[] merchants = entry.getValue();

            // Create category
            Category category = new Category(categoryName, BigDecimal.ZERO);
            category = categoryRepo.save(category);

            // Create merchant rules
            for (String merchant : merchants) {
                MerchantRule rule = new MerchantRule(merchant, category);
                ruleRepo.save(rule);
            }
        }

        // Create Miscellaneous category
        Category miscellaneous = new Category("Miscellaneous", BigDecimal.ZERO);
        categoryRepo.save(miscellaneous);

        System.out.println("✅ Initialized " + categoryMerchants.size() + " categories with merchant rules");
    }
}
