package com.fileload.service.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecordCountUtil {

    private static final List<String> EXPECTED_HEADER = List.of(
            "ProductId", "Name", "Category", "Quantity", "Price"
    );

    public ProcessingResult analyzeFile(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (!fileName.endsWith(".csv")) {
            return ProcessingResult.failed(0, List.of("Invalid file type. Only CSV files are accepted."));
        }

        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        if (content.isBlank()) {
            return ProcessingResult.failed(0, List.of("File is empty."));
        }

        return analyzeCsv(content);
    }

    public long countRecords(Path filePath) throws IOException {
        return analyzeFile(filePath).recordCount();
    }

    private ProcessingResult analyzeCsv(String content) {
        CsvReadResult csv = splitCsvRows(content);
        List<String> rows = csv.rows();
        List<String> errors = new ArrayList<>();

        if (csv.unbalancedQuotes()) {
            errors.add("CSV contains unbalanced quotes.");
        }

        if (rows.isEmpty()) {
            errors.add("File is empty.");
            return ProcessingResult.failed(0, errors);
        }

        List<String> header = splitCsvCells(rows.get(0));
        if (!header.equals(EXPECTED_HEADER)) {
            errors.add("Invalid header. Expected: ProductId,Name,Category,Quantity,Price");
            return ProcessingResult.failed(0, errors);
        }

        int validRecords = 0;
        for (int i = 1; i < rows.size(); i++) {
            String row = rows.get(i);
            if (row.isBlank()) {
                continue;
            }

            List<String> cells = splitCsvCells(row);
            int lineNo = i + 1;

            if (cells.size() != 5) {
                errors.add("Invalid column count at line " + lineNo + ": expected 5, found " + cells.size());
                continue;
            }

            String productId = cells.get(0).trim();
            String name = cells.get(1).trim();
            String categoryText = cells.get(2).trim();
            String quantityText = cells.get(3).trim();
            String priceText = cells.get(4).trim();

            if (productId.isEmpty()) {
                errors.add("Empty ProductId at line " + lineNo);
                continue;
            }
            if (name.isEmpty()) {
                errors.add("Empty Name at line " + lineNo);
                continue;
            }
            if (categoryText.isEmpty()) {
                errors.add("Empty Category at line " + lineNo);
                continue;
            }
            if (quantityText.isEmpty()) {
                errors.add("Empty Quantity at line " + lineNo);
                continue;
            }
            if (priceText.isEmpty()) {
                errors.add("Empty Price at line " + lineNo);
                continue;
            }

            String category = categoryText.toUpperCase();
            if (!"MOBILE".equals(category) && !"LAPTOP".equals(category) && !"TV".equals(category)) {
                errors.add("Invalid Category at line " + lineNo + ": must be Mobile, Laptop, or TV");
                continue;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(quantityText);
            } catch (NumberFormatException ex) {
                errors.add("Invalid Quantity at line " + lineNo);
                continue;
            }
            if (quantity <= 0) {
                errors.add("Invalid Quantity at line " + lineNo + ": must be greater than 0");
                continue;
            }

            double price;
            try {
                price = Double.parseDouble(priceText);
            } catch (NumberFormatException ex) {
                errors.add("Invalid Price at line " + lineNo);
                continue;
            }
            if (price <= 0) {
                errors.add("Invalid Price at line " + lineNo + ": must be greater than 0");
                continue;
            }

            validRecords++;
        }

        if (!errors.isEmpty()) {
            return ProcessingResult.failed(0, errors);
        }

        return ProcessingResult.success(validRecords);
    }

    private CsvReadResult splitCsvRows(String content) {
        List<String> rows = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            char next = i + 1 < content.length() ? content.charAt(i + 1) : '\0';

            if (c == '"') {
                if (inQuotes && next == '"') {
                    current.append(c);
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (c == '\r') {
                continue;
            }

            if (c == '\n' && !inQuotes) {
                String row = current.toString().trim();
                if (!row.isEmpty()) {
                    rows.add(row);
                }
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            rows.add(tail);
        }
        return new CsvReadResult(rows, inQuotes);
    }

    private List<String> splitCsvCells(String row) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            char next = i + 1 < row.length() ? row.charAt(i + 1) : '\0';

            if (c == '"') {
                if (inQuotes && next == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (c == ',' && !inQuotes) {
                cells.add(current.toString().trim());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        cells.add(current.toString().trim());
        return cells;
    }


    private record CsvReadResult(List<String> rows, boolean unbalancedQuotes) {
    }

    public static final class ProcessingResult {
        private final long recordCount;
        private final List<String> errors;

        private ProcessingResult(long recordCount, List<String> errors) {
            this.recordCount = recordCount;
            this.errors = errors;
        }

        public static ProcessingResult success(long recordCount) {
            return new ProcessingResult(recordCount, Collections.emptyList());
        }

        public static ProcessingResult failed(long recordCount, List<String> errors) {
            return new ProcessingResult(recordCount, List.copyOf(errors));
        }

        public long recordCount() {
            return recordCount;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public String errorMessage() {
            return String.join(" ", errors);
        }
    }
}

