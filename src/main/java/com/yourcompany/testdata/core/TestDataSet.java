package com.yourcompany.testdata.core;

import com.yourcompany.testdata.generator.ValueGenerator;
import com.yourcompany.testdata.override.ColumnOverride;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the configuration for generating test rows for a single table.
 * Built via {@link TestDataSet#builder()}; one instance per test invocation.
 */
@Builder
@Getter
public class TestDataSet {

    private final String schema;
    private final String tableName;
    private final int rowCount;
    @Singular
    private final List<ColumnOverride> overrides;
    private final List<ColumnMetadata> columns;
    private final ValueGenerator valueGenerator;

    /** Generates {@code rowCount} rows applying any configured overrides. */
    public List<GeneratedRow> generate() {
        int count = rowCount > 0 ? rowCount : 1;
        List<GeneratedRow> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (ColumnMetadata col : columns) {
                String upperName = col.columnName().toUpperCase();
                ColumnOverride override = findOverride(upperName);
                values.put(upperName, override != null
                        ? override.nextValue()
                        : valueGenerator.generate(col));
            }
            rows.add(new GeneratedRow(values));
        }
        return rows;
    }

    private ColumnOverride findOverride(String upperName) {
        for (ColumnOverride o : overrides) {
            if (o.getColumnName().equals(upperName)) return o;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Custom builder extension — Lombok generates all field setters;
    // we add `reader` (build-time only) and a custom build() that resolves columns.
    // -------------------------------------------------------------------------

    public static class TestDataSetBuilder {

        /** Build-time dependency; not stored on the {@link TestDataSet}. */
        private TableMetadataReader reader;

        public TestDataSetBuilder reader(TableMetadataReader reader) {
            this.reader = reader;
            return this;
        }

        /**
         * Eagerly reads column metadata and constructs the {@link TestDataSet}.
         * Lombok does not generate {@code build()} when it is defined here.
         *
         * @throws SQLException             if the metadata read fails
         * @throws IllegalStateException    if {@code tableName} was not set
         */
        public TestDataSet build() throws SQLException {
            if (tableName == null || tableName.isBlank()) {
                throw new IllegalStateException("table name must be set");
            }
            List<ColumnMetadata> resolvedColumns = reader.read(schema, tableName);
            List<ColumnOverride> builtOverrides =
                    overrides != null ? List.copyOf(overrides) : List.of();
            int effectiveRowCount = rowCount > 0 ? rowCount : 1;
            return new TestDataSet(schema, tableName, effectiveRowCount,
                    builtOverrides, resolvedColumns, valueGenerator);
        }
    }
}
