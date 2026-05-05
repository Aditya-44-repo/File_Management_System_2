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
    void shouldCountTradeCsvRecordsWithoutErrors() throws Exception {
        Path file = tempDir.resolve("valid.csv");
        Files.writeString(file,
                "tradeId,clientId,stockSymbol,quantity,price,tradeType\n"
                        + "T1,C1,AAPL,10,189.50,BUY\n"
                        + "T2,C2,MSFT,5,320.25,sell\n");

        RecordCountUtil.ProcessingResult result = recordCountUtil.analyzeFile(file);

        assertEquals(2L, result.recordCount());
        assertFalse(result.hasErrors());
    }

    @Test
    void shouldFailWhenHeaderDoesNotMatch() throws Exception {
        Path file = tempDir.resolve("invalid-columns.csv");
        Files.writeString(file,
                "id,name,qty,price,type,client\n"
                        + "T1,C1,AAPL,10,189.50,BUY\n");

        RecordCountUtil.ProcessingResult result = recordCountUtil.analyzeFile(file);

        assertEquals(0L, result.recordCount());
        assertTrue(result.hasErrors());
        assertTrue(result.errorMessage().contains("Invalid header"));
    }

    @Test
    void shouldFailWhenQuantityInvalid() throws Exception {
        Path file = tempDir.resolve("invalid-quantity.csv");
        Files.writeString(file,
                "tradeId,clientId,stockSymbol,quantity,price,tradeType\n"
                        + "T1,C1,AAPL,-1,189.50,BUY\n");

        RecordCountUtil.ProcessingResult result = recordCountUtil.analyzeFile(file);

        assertTrue(result.hasErrors());
        assertTrue(result.errorMessage().contains("Invalid quantity at line 2"));
    }

    @Test
    void shouldFailWhenTradeTypeInvalid() throws Exception {
        Path file = tempDir.resolve("invalid-tradetype.csv");
        Files.writeString(file,
                "tradeId,clientId,stockSymbol,quantity,price,tradeType\n"
                        + "T1,C1,AAPL,1,189.50,HOLD\n");

        RecordCountUtil.ProcessingResult result = recordCountUtil.analyzeFile(file);

        assertTrue(result.hasErrors());
        assertTrue(result.errorMessage().contains("Invalid tradeType at line 2"));
    }
}

