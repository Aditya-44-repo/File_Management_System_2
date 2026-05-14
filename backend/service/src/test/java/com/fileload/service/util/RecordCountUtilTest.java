package com.fileload.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordCountUtilTest {

    private final RecordCountUtil recordCountUtil = new RecordCountUtil();

    @TempDir
    Path tempDir;

    @Test
    void shouldCountProductCsvRecordsWithoutErrors() throws Exception {
        Path file = tempDir.resolve("valid.csv");
        Files.writeString(file,
                "ProductId,Name,Category,Quantity,Price\n"
                        + "P1,Samsung Galaxy,Mobile,10,19999\n"
                        + "P2,MacBook Pro,laptop,5,209999\n");

        RecordCountUtil.ProcessingResult result = recordCountUtil.analyzeFile(file);

        assertEquals(2L, result.recordCount());
        assertFalse(result.hasErrors());
    }

    @Test
    void shouldFailWhenHeaderDoesNotMatch() throws Exception {
        Path file = tempDir.resolve("invalid-columns.csv");
        Files.writeString(file,
                "id,name,qty,price,category\n"
                        + "P1,Samsung Galaxy,Mobile,10,19999\n");

        RecordCountUtil.ProcessingResult result = recordCountUtil.analyzeFile(file);

        assertEquals(0L, result.recordCount());
        assertTrue(result.hasErrors());
        assertTrue(result.errorMessage().contains("Invalid header"));
    }

    @Test
    void shouldFailWhenQuantityInvalid() throws Exception {
        Path file = tempDir.resolve("invalid-quantity.csv");
        Files.writeString(file,
                "ProductId,Name,Category,Quantity,Price\n"
                        + "P1,Samsung Galaxy,Mobile,-1,19999\n");

        RecordCountUtil.ProcessingResult result = recordCountUtil.analyzeFile(file);

        assertTrue(result.hasErrors());
        assertTrue(result.errorMessage().contains("Invalid Quantity at line 2"));
    }

    @Test
    void shouldFailWhenCategoryInvalid() throws Exception {
        Path file = tempDir.resolve("invalid-category.csv");
        Files.writeString(file,
                "ProductId,Name,Category,Quantity,Price\n"
                        + "P1,Samsung Galaxy,Camera,1,19999\n");

        RecordCountUtil.ProcessingResult result = recordCountUtil.analyzeFile(file);

        assertTrue(result.hasErrors());
        assertTrue(result.errorMessage().contains("Invalid Category at line 2"));
    }
}
