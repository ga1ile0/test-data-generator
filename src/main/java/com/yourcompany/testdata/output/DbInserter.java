package com.yourcompany.testdata.output;

import com.yourcompany.testdata.core.ColumnMetadata;
import com.yourcompany.testdata.core.GeneratedRow;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility for inserting generated rows into the database via plain JDBC.
 * Thread-safe: borrows a connection per call from the pool.
 */
public final class DbInserter {

    private DbInserter() {}

    /**
     * Executes one {@code INSERT} per row using a {@link PreparedStatement}.
     *
     * @param dataSource JDBC data source
     * @param schema     Oracle schema; may be {@code null} to use the session default
     * @param tableName  target table name
     * @param rows       generated rows to insert
     * @param columns    ordered column metadata (determines insert column order)
     * @throws SQLException if any INSERT fails
     */
    public static void insert(DataSource dataSource, String schema, String tableName,
                              List<GeneratedRow> rows, List<ColumnMetadata> columns)
            throws SQLException {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (GeneratedRow row : rows) {
                int idx = 1;
                for (ColumnMetadata col : columns) {
                    ps.setObject(idx++, row.get(col.columnName()));
                }
                ps.executeUpdate();
            }
        }
    }
}
