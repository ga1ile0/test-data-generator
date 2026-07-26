package com.yourcompany.testdata.generator;

import com.yourcompany.testdata.core.ColumnMetadata;
import net.datafaker.Faker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Generates realistic, Oracle-constraint-aware column values using DataFaker.
 *
 * <p>Thread safety: a {@link ThreadLocal} ensures each thread has its own
 * {@link Faker} instance, so this class is safe for concurrent use.
 */
public class OracleValueGenerator implements ValueGenerator {

    private static final ThreadLocal<Faker> FAKER =
            ThreadLocal.withInitial(() -> new Faker(Locale.ENGLISH));

    @Override
    public Object generate(ColumnMetadata col) {
        Faker faker = FAKER.get();
        return switch (col.sqlType()) {
            case Types.VARCHAR, Types.NVARCHAR -> {
                int size = col.columnSize() > 0 ? Math.min(col.columnSize(), 200) : 50;
                yield faker.lorem().characters(1, size, true, true);
            }
            case Types.CHAR, Types.NCHAR -> {
                int size = Math.max(1, col.columnSize());
                yield faker.lorem().characters(size, size);
            }
            case Types.NUMERIC, Types.DECIMAL -> generateNumber(col, faker);
            case Types.INTEGER, Types.BIGINT, Types.SMALLINT, Types.TINYINT ->
                    faker.number().randomNumber(8, false);
            case Types.FLOAT, Types.REAL, Types.DOUBLE ->
                    faker.number().randomDouble(4, -9999999, 9999999);
            case Types.DATE ->
                    new java.sql.Date(faker.timeAndDate().past(365L * 5, TimeUnit.DAYS).toEpochMilli());
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE ->
                    new java.sql.Timestamp(faker.timeAndDate().past(365L * 5, TimeUnit.DAYS).toEpochMilli());
            case Types.CLOB, Types.NCLOB, Types.LONGVARCHAR, Types.LONGNVARCHAR ->
                    faker.lorem().paragraph(2);
            case Types.BLOB, Types.BINARY, Types.VARBINARY -> {
                int size = Math.min(Math.max(1, col.columnSize()), 16);
                byte[] bytes = new byte[size];
                ThreadLocalRandom.current().nextBytes(bytes);
                yield bytes;
            }
            case Types.SQLXML -> "<root/>";
            default -> faker.lorem().word();
        };
    }

    private static BigDecimal generateNumber(ColumnMetadata col, Faker faker) {
        int precision = col.columnSize() > 0 ? col.columnSize() : 10;
        int scale = col.decimalDigits() >= 0 ? col.decimalDigits() : 2;

        if (scale == 0) {
            int intDigits = Math.max(1, precision);
            long max = (long) (Math.pow(10, intDigits) - 1);
            return BigDecimal.valueOf(faker.number().numberBetween(0L, max));
        }

        int intDigits = Math.max(1, precision - scale);
        long max = (long) (Math.pow(10, intDigits) - 1);
        long intPart = faker.number().numberBetween(0L, max);
        long decPart = faker.number().numberBetween(0L, (long) (Math.pow(10, scale) - 1));
        String raw = intPart + "." + String.format("%0" + scale + "d", decPart);
        return new BigDecimal(raw).setScale(scale, RoundingMode.HALF_UP);
    }
}
