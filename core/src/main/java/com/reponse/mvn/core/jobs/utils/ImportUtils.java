package com.reponse.mvn.core.jobs.utils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Stateless utility methods shared across the course-import pipeline.
 */
public final class ImportUtils {

    // ── Supported date formats (tried in order) ───────────────────────────────

    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd",
        "MM/dd/yyyy",
        "d/M/yyyy",
        "MMMM d, yyyy",
        "MMMM d yyyy",
        "MMM d, yyyy",
        "MMM d yyyy",
        "d MMM yyyy",
        "d-MMM-yyyy"
    };

    // ── Column-header aliases (lowercased) ────────────────────────────────────

    public static final Map<String, String> COLUMN_ALIASES = new HashMap<>();
    static {
        COLUMN_ALIASES.put("start_date",     "startdate");
        COLUMN_ALIASES.put("start date",     "startdate");
        COLUMN_ALIASES.put("file_reference", "filereference");
        COLUMN_ALIASES.put("file reference", "filereference");
    }

    private ImportUtils() {}

    // ── Row mapping ───────────────────────────────────────────────────────────

    public static List<Map<String, String>> rowsToMaps(List<String[]> allRows) {
        if (allRows.isEmpty()) return new ArrayList<>();
        String[] rawHeaders = allRows.get(0);
        String[] headers = new String[rawHeaders.length];
        for (int i = 0; i < rawHeaders.length; i++) {
            String h = (rawHeaders[i] == null ? "" : rawHeaders[i]).trim().toLowerCase(Locale.ENGLISH);
            headers[i] = COLUMN_ALIASES.getOrDefault(h, h);
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (int r = 1; r < allRows.size(); r++) {
            String[] row = allRows.get(r);
            Map<String, String> map = new LinkedHashMap<>();
            for (int c = 0; c < headers.length; c++) {
                map.put(headers[c], (c < row.length && row[c] != null) ? row[c].trim() : "");
            }
            result.add(map);
        }
        return result;
    }

    // ── Slug ──────────────────────────────────────────────────────────────────

    public static String generateSlug(String title) {
        String s = title.toLowerCase(Locale.ENGLISH)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "course-" + System.currentTimeMillis() : s;
    }

    public static String findUniquePagePath(Session session, String parent, String baseSlug)
            throws RepositoryException {
        String candidate = parent + "/" + baseSlug;
        if (!session.nodeExists(candidate)) return candidate;
        for (int i = 1; i < 1000; i++) {
            candidate = parent + "/" + baseSlug + "-" + i;
            if (!session.nodeExists(candidate)) return candidate;
        }
        return parent + "/" + baseSlug + "-" + System.currentTimeMillis();
    }

    // ── Date parsing ──────────────────────────────────────────────────────────

    public static Calendar parseDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        raw = raw.trim();
        for (String pattern : DATE_PATTERNS) {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
            sdf.setLenient(false);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            ParsePosition pos = new ParsePosition(0);
            java.util.Date parsed = sdf.parse(raw, pos);
            if (parsed != null && pos.getIndex() == raw.length()) {
                GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                cal.setTime(parsed);
                return cal;
            }
        }
        return null;
    }

    // ── Tags ──────────────────────────────────────────────────────────────────

    public static String[] normalizeTags(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new String[0];
        String[] parts = raw.split("[;|]+");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            p = p.trim();
            if (!p.isEmpty()) {
                result.add(p.contains(":") ? p : "academy:topic/" + p);
            }
        }
        return result.toArray(new String[0]);
    }

    // ── Text ──────────────────────────────────────────────────────────────────

    public static String toHtml(String text) {
        if (text == null || text.trim().isEmpty()) return "<p></p>";
        if (text.trim().startsWith("<")) return text;
        return "<p>" + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</p>";
    }

    public static String nvl(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s;
    }
}
