package com.aryan.smartexpense.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        // If DATABASE_URL environment variable is set, use PostgreSQL
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            return configurePostgreSQLDataSource(databaseUrl);
        }
        // Otherwise use default H2 configuration from application.properties
        return properties.initializeDataSourceBuilder().build();
    }

    private DataSource configurePostgreSQLDataSource(String databaseUrl) {
        // Parse DATABASE_URL format: postgresql://user:password@host:port/dbname
        String url = databaseUrl.replace("postgresql://", "jdbc:postgresql://");

        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl(url);
        properties.setDriverClassName("org.postgresql.Driver");

        // Extract username and password from URL if present
        try {
            String[] parts = databaseUrl.replace("postgresql://", "").split("@");
            if (parts.length == 2) {
                String[] credentials = parts[0].split(":");
                if (credentials.length == 2) {
                    properties.setUsername(credentials[0]);
                    properties.setPassword(credentials[1]);
                }
            }
        } catch (Exception e) {
            // If parsing fails, rely on environment variables for credentials
            properties.setUsername(System.getenv().getOrDefault("DB_USERNAME", "postgres"));
            properties.setPassword(System.getenv().getOrDefault("DB_PASSWORD", ""));
        }

        return properties.initializeDataSourceBuilder().build();
    }
}
