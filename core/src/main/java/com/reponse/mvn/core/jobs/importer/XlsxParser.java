package com.reponse.mvn.core.jobs.importer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Zero-dependency XLSX parser using ZIP + SAX.
 * Reads the first worksheet only. Returns raw rows as {@code List<String[]>} — header row included as row 0.
 */
public final class XlsxParser {

    private XlsxParser() {}

    public static List<String[]> parse(InputStream is) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if ("xl/sharedStrings.xml".equals(name) || name.startsWith("xl/worksheets/sheet")) {
                    entries.put(name, readAllBytes(zip));
                }
                zip.closeEntry();
            }
        }

        List<String> sharedStrings = new ArrayList<>();
        byte[] ssBytes = entries.get("xl/sharedStrings.xml");
        if (ssBytes != null) sharedStrings = parseSharedStrings(ssBytes);

        byte[] sheetBytes = entries.get("xl/worksheets/sheet1.xml");
        if (sheetBytes == null) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                if (e.getKey().startsWith("xl/worksheets/sheet")) { sheetBytes = e.getValue(); break; }
            }
        }
        if (sheetBytes == null) throw new IOException("No worksheet found in XLSX file");

        return parseSheet(sheetBytes, sharedStrings);
    }

    private static byte[] readAllBytes(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = zip.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    private static List<String> parseSharedStrings(byte[] xml) throws Exception {
        final List<String> strings = new ArrayList<>();
        newSaxParser().parse(new ByteArrayInputStream(xml), new DefaultHandler() {
            boolean inSi, inT;
            final StringBuilder buf = new StringBuilder();

            @Override public void startElement(String u, String l, String q, Attributes a) {
                if ("si".equalsIgnoreCase(q))             { inSi = true; buf.setLength(0); }
                else if ("t".equalsIgnoreCase(q) && inSi) { inT = true; }
            }
            @Override public void characters(char[] ch, int s, int len) {
                if (inT) buf.append(ch, s, len);
            }
            @Override public void endElement(String u, String l, String q) {
                if ("t".equalsIgnoreCase(q))       { inT = false; }
                else if ("si".equalsIgnoreCase(q)) { strings.add(buf.toString()); inSi = false; }
            }
        });
        return strings;
    }

    private static List<String[]> parseSheet(byte[] xml, final List<String> ss) throws Exception {
        final List<String[]> rows = new ArrayList<>();
        newSaxParser().parse(new ByteArrayInputStream(xml), new DefaultHandler() {
            List<String> currentRow;
            String cellRef = "", cellType = "";
            final StringBuilder cellVal = new StringBuilder();

            @Override public void startElement(String u, String l, String q, Attributes a) {
                if ("row".equalsIgnoreCase(q)) {
                    currentRow = new ArrayList<>();
                } else if ("c".equalsIgnoreCase(q)) {
                    cellRef  = a.getValue("r") != null ? a.getValue("r") : "";
                    cellType = a.getValue("t") != null ? a.getValue("t") : "";
                    cellVal.setLength(0);
                }
            }
            @Override public void characters(char[] ch, int s, int len) {
                cellVal.append(ch, s, len);
            }
            @Override public void endElement(String u, String l, String q) {
                if ("v".equalsIgnoreCase(q) && currentRow != null) {
                    String raw = cellVal.toString().trim();
                    String resolved = raw;
                    if ("s".equals(cellType)) {
                        try {
                            int idx = Integer.parseInt(raw);
                            resolved = (idx >= 0 && idx < ss.size()) ? ss.get(idx) : "";
                        } catch (NumberFormatException e) { /* keep raw */ }
                    }
                    int col = colIndex(cellRef);
                    if (col >= 0) {
                        while (currentRow.size() <= col) currentRow.add("");
                        currentRow.set(col, resolved);
                    }
                } else if ("row".equalsIgnoreCase(q) && currentRow != null) {
                    if (!currentRow.isEmpty()) rows.add(currentRow.toArray(new String[0]));
                    currentRow = null;
                }
            }
        });
        return rows;
    }

    /** Converts "AB12" → zero-based column index (bijective base-26). */
    private static int colIndex(String ref) {
        if (ref == null || ref.isEmpty()) return -1;
        int result = 0;
        for (int i = 0; i < ref.length(); i++) {
            char c = ref.charAt(i);
            if (!Character.isLetter(c)) break;
            result = result * 26 + (Character.toUpperCase(c) - 'A' + 1);
        }
        return result - 1;
    }

    private static SAXParser newSaxParser() throws Exception {
        SAXParserFactory f = SAXParserFactory.newInstance();
        f.setNamespaceAware(false);
        return f.newSAXParser();
    }
}
