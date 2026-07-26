package com.yourcompany.testdata.output;

import com.yourcompany.testdata.core.ColumnMetadata;
import com.yourcompany.testdata.core.GeneratedRow;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility for exporting generated rows as Oracle-compatible SQL INSERT statements.
 * Thread-safe: no shared mutable state.
 */
public final class SqlScriptExporter {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private SqlScriptExporter() {}

    /**
     * Writes Oracle INSERT statements for all rows to {@code outputPath}.
     * The file is created or truncated on each call.
     *
     * @param schema     Oracle schema; may be {@code null}
     * @param tableName  target table name
     * @param rows       generated rows
     * @param columns    ordered column metadata
     * @param outputPath destination file path
     * @throws IOException if the file cannot be written
     */
    public static void export(String schema, String tableName,
                              List<GeneratedRow> rows, List<ColumnMetadata> columns,
                              Path outputPath) throws IOException {
        String tableRef = schema != null ? schema + "." + tableName : tableName;
        String colList = columns.stream()
                .map(ColumnMetadata::columnName)
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        for (GeneratedRow row : rows) {
            String values = columns.stream()
                    .map(col -> toLiteral(row.get(col.columnName())))
                    .collect(Collectors.joining(", "));
            sb.append("INSERT INTO ")
              .append(tableRef)
              .append(" (").append(colList).append(")")
              .append(" VALUES (").append(values).append(");\n");
        }
        Files.writeString(outputPath, sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String toLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof byte[] bytes) {
            return "HEXTORAW('" + HexFormat.of().formatHex(bytes).toUpperCase() + "')";
        }
        if (value instanceof Timestamp ts) {
            String formatted = ts.toLocalDateTime().format(TS_FMT);
            return "TO_TIMESTAMP('" + formatted + "', 'YYYY-MM-DD HH24:MI:SS.FF3')";
        }
        if (value instanceof Date date) {
            String formatted = date.toLocalDate().format(DATE_FMT);
            return "TO_DATE('" + formatted + "', 'YYYY-MM-DD')";
        }
        if (value instanceof BigDecimal || value instanceof Number) {
            return value.toString();
        }
        if (value instanceof String str) {
            return "'" + str.replace("'", "''") + "'";
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
}
