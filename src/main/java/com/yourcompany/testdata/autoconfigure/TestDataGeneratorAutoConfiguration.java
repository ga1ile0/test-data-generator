package com.yourcompany.testdata.autoconfigure;

import com.yourcompany.testdata.TestDataGenerator;
import com.yourcompany.testdata.config.TestDataGeneratorProperties;
import com.yourcompany.testdata.core.TableMetadataReader;
import com.yourcompany.testdata.generator.OracleValueGenerator;
import com.yourcompany.testdata.generator.ValueGenerator;
import com.yourcompany.testdata.output.DbInserter;
import com.yourcompany.testdata.output.SqlScriptExporter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Spring Boot auto-configuration for the test-data-generator library.
 * Activated when a {@link DataSource} bean is present on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass({DataSource.class, JdbcTemplate.class})
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(TestDataGeneratorProperties.class)
public class TestDataGeneratorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TableMetadataReader tableMetadataReader(DataSource dataSource) {
        return new TableMetadataReader(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public ValueGenerator oracleValueGenerator() {
        return new OracleValueGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public DbInserter dbInserter(JdbcTemplate jdbcTemplate) {
        return new DbInserter(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlScriptExporter sqlScriptExporter() {
        return new SqlScriptExporter();
    }

    @Bean
    @ConditionalOnMissingBean
    public TestDataGenerator testDataGenerator(TableMetadataReader reader,
                                               ValueGenerator generator,
                                               DbInserter inserter,
                                               SqlScriptExporter exporter) {
        return new TestDataGenerator(reader, generator, inserter, exporter);
    }
}
