package com.aidb.aidb_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class QueryUserJdbcConfig {
    @Value("${user.sql.datasource.url}")
    private String dataSourceUrl;

    @Value("${user.sql.datasource.username}")
    private String dataSourceUsername;

    @Value("${user.sql.datasource.password}")
    private String dataSourcePassword;

    @Value("${user.sql.datasource.driver-class-name}")
    private String dataSourceDriverClassName;

    @Bean(name = "queryUserDataSource")
    public DataSource queryUserDataSource() {
        return DataSourceBuilder.create()
                .url(dataSourceUrl)
                .username(dataSourceUsername)
                .password(dataSourcePassword)
                .driverClassName(dataSourceDriverClassName)
                .build();
    }

    @Bean(name = "queryUserJdbcTemplate")
    public JdbcTemplate queryUserJdbcTemplate() {
        return new JdbcTemplate(queryUserDataSource());
    }
}
