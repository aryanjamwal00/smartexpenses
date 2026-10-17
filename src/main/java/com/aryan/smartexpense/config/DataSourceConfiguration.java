package com.aryan.smartexpense.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataSourceConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String url = properties.getUrl();
        if (url != null && (url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
            URI uri = URI.create(url);
            String userInfo = uri.getRawUserInfo();
            String username = userInfo == null ? null : decode(userInfo.split(":", 2)[0]);
            String password = userInfo == null || !userInfo.contains(":")
                    ? null : decode(userInfo.substring(userInfo.indexOf(':') + 1));
            String database = uri.getPath() == null ? "" : uri.getPath();
            properties.setUrl("jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + database);
            if (username != null) properties.setUsername(username);
            if (password != null) properties.setPassword(password);
        }
        return properties.initializeDataSourceBuilder().build();
    }

    private String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
