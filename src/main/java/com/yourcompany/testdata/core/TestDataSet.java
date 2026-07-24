package com.yourcompany.testdata.core;

import com.yourcompany.testdata.generator.ValueGenerator;
import com.yourcompany.testdata.override.ColumnOverride;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the configuration for generating test rows for a single table.
 * Built via its inner {@link Builder}; one instance per test invocation.
 */
public class TestDataSet {

    private final String schema;
    private final String tableName;
    private final int rowCount;
    private final List<ColumnOverride> overrides;
    private final List<ColumnMetadata> columns;
    private final ValueGenerator valueGenerator;

    private TestDataSet(Builder builder) {
        this.schema = builder.schema;
        this.tableName = builder.tableName;
        this.rowCount = builder.rowCount;
        this.overrides = List.copyOf(builder.overrides);
        this.columns = builder.columns;
        this.valueGenerator = builder.valueGenerator;
    }

    /** Generates {@code rowCount} rows applying any configured overrides. */
    public List<GeneratedRow> generate() {
        List<GeneratedRow> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (ColumnMetadata col : columns) {
                String upperName = col.columnName().toUpperCase();
                ColumnOverride override = findOverride(upperName);
                if (override != null) {
                    values.put(upperName, override.nextValue());
                } else {
                    values.put(upperName, valueGenerator.generate(col));
                }
            }
            rows.add(new GeneratedRow(values));
        }
        return rows;
    }

    private ColumnOverride findOverride(String upperName) {
        for (ColumnOverride o : overrides) {
            if (o.getColumnName().equals(upperName)) {
                return o;
            }
        }
        return null;
    }

    public String getSchema() { return schema; }
    public String getTableName() { return tableName; }
    public List<ColumnMetadata> getColumns() { return columns; }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static class Builder {

        private String tableName;
        private String schema;
        private int rowCount = 1;
        private final List<ColumnOverride> overrides = new ArrayList<>();

        // injected by TestDataGenerator
        private TableMetadataReader reader;
        private ValueGenerator valueGenerator;

        // eagerly resolved on build()
        private List<ColumnMetadata> columns;

        public Builder table(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder rows(int count) {
            this.rowCount = count;
            return this;
        }

        public Builder override(ColumnOverride override) {
            this.overrides.add(override);
            return this;
        }

        public Builder overrides(List<ColumnOverride> overrides) {
            this.overrides.addAll(overrides);
            return this;
        }

        /** For internal use by {@link com.yourcompany.testdata.TestDataGenerator}. */
        public Builder reader(TableMetadataReader reader) {
            this.reader = reader;
            return this;
        }

        /** For internal use by {@link com.yourcompany.testdata.TestDataGenerator}. */
        public Builder generator(ValueGenerator generator) {
            this.valueGenerator = generator;
            return this;
        }

        /**
         * Eagerly reads column metadata and constructs the {@link TestDataSet}.
         *
         * @throws SQLException if the metadata read fails
         */
        public TestDataSet build() throws SQLException {
            if (tableName == null || tableName.isBlank()) {
                throw new IllegalStateException("table name must be set");
            }
            this.columns = reader.read(schema, tableName);
            return new TestDataSet(this);
        }
    }
}
