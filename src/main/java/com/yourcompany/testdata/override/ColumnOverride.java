package com.yourcompany.testdata.override;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Encapsulates a per-column value override for test data generation.
 *
 * <p>Create instances via the static factory methods; do not call the constructor directly.
 */
public class ColumnOverride {

    private final String columnName;
    private final Supplier<Object> valueSupplier;

    private ColumnOverride(String columnName, Supplier<Object> valueSupplier) {
        this.columnName = columnName.toUpperCase();
        this.valueSupplier = valueSupplier;
    }

    /** Always returns the same fixed value for every generated row. */
    public static ColumnOverride fixed(String columnName, Object value) {
        return new ColumnOverride(columnName, () -> value);
    }

    /**
     * Calls {@code supplier} for each generated row.
     * The caller is responsible for thread safety of the supplier.
     */
    public static ColumnOverride supplied(String columnName, Supplier<Object> supplier) {
        return new ColumnOverride(columnName, supplier);
    }

    /** Generates a fresh {@link UUID} string for each row. */
    public static ColumnOverride uniqueUuid(String columnName) {
        return new ColumnOverride(columnName, () -> UUID.randomUUID().toString());
    }

    /**
     * Returns a monotonically incrementing long (as a {@link Long}) starting at
     * {@code startValue}. Thread-safe: backed by an {@link AtomicLong}.
     */
    public static ColumnOverride uniqueSequence(String columnName, long startValue) {
        AtomicLong counter = new AtomicLong(startValue);
        return new ColumnOverride(columnName, counter::getAndIncrement);
    }

    /** Column name, always upper-cased. */
    public String getColumnName() {
        return columnName;
    }

    /** Retrieves the next value for this column. */
    public Object nextValue() {
        return valueSupplier.get();
    }
}
