package com.reponse.mvn.core.jobs.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RFC 4180-compliant CSV parser with quoted-field and multi-line support.
 * Returns raw rows as {@code List<String[]>} — header row included as row 0.
 */
public final class CsvParser {

    private CsvParser() {}

    public static List<String[]> parse(InputStream is) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            StringBuilder pending = null;
            while ((line = reader.readLine()) != null) {
                if (pending != null) {
                    pending.append('\n').append(line);
                    if (countQuotes(pending.toString()) % 2 == 0) {
                        rows.add(splitLine(pending.toString()));
                        pending = null;
                    }
                } else if (countQuotes(line) % 2 != 0) {
                    pending = new StringBuilder(line);
                } else {
                    rows.add(splitLine(line));
                }
            }
        }
        return rows;
    }

    private static int countQuotes(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '"') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '"') { i++; }
                else { count++; }
            }
        }
        return count;
    }

    private static String[] splitLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
