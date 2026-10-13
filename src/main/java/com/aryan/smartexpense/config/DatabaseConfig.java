package com.aryan.smartexpense.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        // If DATABASE_URL environment variable is set, use PostgreSQL
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            return configurePostgreSQLDataSource(databaseUrl);
        }
        // Otherwise use default H2 configuration from application.properties
        // Spring Boot will auto-configure H2 when DATABASE_URL is not set
        return null;
    }

    private DataSource configurePostgreSQLDataSource(String databaseUrl) {
        // Parse DATABASE_URL format: postgresql://user:password@host:port/dbname
        String url = databaseUrl.replace("postgresql://", "jdbc:postgresql://");

        String username = "postgres";
        String password = "";

        // Extract username and password from URL if present
        try {
            String[] parts = databaseUrl.replace("postgresql://", "").split("@");
            if (parts.length == 2) {
                String[] credentials = parts[0].split(":");
                if (credentials.length == 2) {
                    username = credentials[0];
                    password = credentials[1];
                }
            }
        } catch (Exception e) {
            // If parsing fails, rely on environment variables for credentials
            username = System.getenv().getOrDefault("DB_USERNAME", "postgres");
            password = System.getenv().getOrDefault("DB_PASSWORD", "");
        }

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
