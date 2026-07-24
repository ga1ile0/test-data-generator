package com.yourcompany.testdata.output;

import com.yourcompany.testdata.core.ColumnMetadata;
import com.yourcompany.testdata.core.GeneratedRow;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Inserts generated rows directly into the database via {@link JdbcTemplate}.
 * Stateless: safe for use as a singleton in concurrent environments.
 */
public class DbInserter {

    private final JdbcTemplate jdbcTemplate;

    public DbInserter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes one {@code INSERT} per row in {@code rows}.
     *
     * @param schema    Oracle schema; may be {@code null} to use the session default
     * @param tableName target table name
     * @param rows      generated rows to insert
     * @param columns   ordered column metadata (determines insert column order)
     */
    public void insert(String schema, String tableName,
                       List<GeneratedRow> rows, List<ColumnMetadata> columns) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String tableRef = schema != null ? schema + "." + tableName : tableName;
        String colList = columns.stream()
                .map(ColumnMetadata::columnName)
                .collect(Collectors.joining(", "));
        String placeholders = columns.stream()
                .map(c -> "?")
                .collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + tableRef + " (" + colList + ") VALUES (" + placeholders + ")";

        for (GeneratedRow row : rows) {
            Object[] args = columns.stream()
                    .map(col -> row.get(col.columnName()))
                    .toArray();
            jdbcTemplate.update(sql, args);
        }
    }
}
