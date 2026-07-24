package com.yourcompany.testdata;

import com.yourcompany.testdata.core.ColumnMetadata;
import com.yourcompany.testdata.generator.OracleValueGenerator;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link OracleValueGenerator}. No Spring context required.
 */
public class OracleValueGeneratorTest {

    private OracleValueGenerator generator;

    @BeforeClass
    public void setUp() {
        generator = new OracleValueGenerator();
    }

    // --- VARCHAR ---

    @Test
    public void testVarcharGeneration() {
        ColumnMetadata col = col("NAME", Types.VARCHAR, "VARCHAR2", 100, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof String, "Expected String");
        assertTrue(((String) result).length() <= 100);
    }

    @Test
    public void testVarcharSmallSize() {
        ColumnMetadata col = col("CODE", Types.VARCHAR, "VARCHAR2", 5, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(((String) result).length() <= 5);
    }

    @Test
    public void testVarcharZeroSize() {
        // columnSize <= 0 should default to 50
        ColumnMetadata col = col("NOTES", Types.VARCHAR, "VARCHAR2", 0, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(((String) result).length() <= 50);
    }

    // --- CHAR ---

    @Test
    public void testCharGeneration() {
        ColumnMetadata col = col("FLAG", Types.CHAR, "CHAR", 3, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof String, "Expected String");
        assertEquals(((String) result).length(), 3);
    }

    // --- NUMERIC / NUMBER with scale 0 ---

    @Test
    public void testNumericScaleZero() {
        ColumnMetadata col = col("AGE", Types.NUMERIC, "NUMBER", 5, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof BigDecimal, "Expected BigDecimal");
        assertEquals(((BigDecimal) result).scale(), 0);
    }

    // --- NUMERIC / NUMBER with scale > 0 ---

    @Test
    public void testNumericWithScale() {
        ColumnMetadata col = col("SALARY", Types.NUMERIC, "NUMBER", 12, 2, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof BigDecimal, "Expected BigDecimal");
        assertEquals(((BigDecimal) result).scale(), 2);
    }

    @Test
    public void testDecimalWithScale() {
        ColumnMetadata col = col("PRICE", Types.DECIMAL, "NUMBER", 10, 4, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof BigDecimal, "Expected BigDecimal");
        assertEquals(((BigDecimal) result).scale(), 4);
    }

    // --- INTEGER types ---

    @Test
    public void testIntegerGeneration() {
        ColumnMetadata col = col("ID", Types.INTEGER, "INTEGER", 10, 0, false);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof Long, "Expected Long");
    }

    @Test
    public void testBigintGeneration() {
        ColumnMetadata col = col("SEQ", Types.BIGINT, "NUMBER", 19, 0, false);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof Long, "Expected Long");
    }

    // --- FLOAT / DOUBLE ---

    @Test
    public void testFloatGeneration() {
        ColumnMetadata col = col("RATE", Types.FLOAT, "FLOAT", 15, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof Double, "Expected Double");
    }

    // --- DATE ---

    @Test
    public void testDateGeneration() {
        ColumnMetadata col = col("BIRTH_DATE", Types.DATE, "DATE", 7, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof java.sql.Date, "Expected java.sql.Date");
    }

    // --- TIMESTAMP ---

    @Test
    public void testTimestampGeneration() {
        ColumnMetadata col = col("CREATED_AT", Types.TIMESTAMP, "TIMESTAMP", 11, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof java.sql.Timestamp, "Expected java.sql.Timestamp");
    }

    @Test
    public void testTimestampWithTimezone() {
        ColumnMetadata col = col("UPDATED_AT", Types.TIMESTAMP_WITH_TIMEZONE, "TIMESTAMP WITH TIME ZONE", 13, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof java.sql.Timestamp, "Expected java.sql.Timestamp");
    }

    // --- CLOB ---

    @Test
    public void testClobGeneration() {
        ColumnMetadata col = col("DESCRIPTION", Types.CLOB, "CLOB", 0, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof String, "Expected String");
        assertFalse(((String) result).isEmpty());
    }

    // --- BLOB ---

    @Test
    public void testBlobGeneration() {
        ColumnMetadata col = col("CONTENT", Types.BLOB, "BLOB", 8, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof byte[], "Expected byte[]");
        byte[] bytes = (byte[]) result;
        assertTrue(bytes.length >= 1 && bytes.length <= 8);
    }

    @Test
    public void testVarbinaryGeneration() {
        ColumnMetadata col = col("HASH", Types.VARBINARY, "RAW", 16, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof byte[], "Expected byte[]");
        assertTrue(((byte[]) result).length <= 16);
    }

    // --- SQLXML ---

    @Test
    public void testSqlXmlGeneration() {
        ColumnMetadata col = col("XML_DATA", Types.SQLXML, "XMLTYPE", 0, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertEquals(result, "<root/>");
    }

    // --- Default fallback ---

    @Test
    public void testDefaultFallback() {
        // Use a rarely-used type to hit the default branch
        ColumnMetadata col = col("MISC", Types.OTHER, "OTHER", 50, 0, true);
        Object result = generator.generate(col);
        assertNotNull(result);
        assertTrue(result instanceof String, "Expected String");
    }

    // --- Thread safety ---

    @Test
    public void testConcurrentGeneration() throws Exception {
        ColumnMetadata varcharCol = col("CONCURRENT", Types.VARCHAR, "VARCHAR2", 100, 0, true);
        int threadCount = 20;
        int callsPerThread = 100;

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean failedFlag = new AtomicBoolean(false);

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            futures.add(pool.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < callsPerThread; i++) {
                        Object result = generator.generate(varcharCol);
                        if (result == null) {
                            failedFlag.set(true);
                        }
                    }
                } catch (Exception e) {
                    failedFlag.set(true);
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertFalse(failedFlag.get(), "A thread received a null result or threw an exception");
    }

    // --- Helper ---

    private static ColumnMetadata col(String name, int sqlType, String typeName,
                                      int size, int scale, boolean nullable) {
        return new ColumnMetadata(name, sqlType, typeName, size, scale, nullable, 1);
    }
}
