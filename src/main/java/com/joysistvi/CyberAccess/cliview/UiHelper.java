package com.joysistvi.CyberAccess.cliview;

import java.util.Arrays;

/**
 * Shared console UI helper for CyberAccess.
 * Keeps menus, headers, messages, and tables visually consistent.
 */
public final class UiHelper {
    private UiHelper() {}

    public static final String RESET = "\u001B[0m";
    public static final String CYAN = "\u001B[96m";
    public static final String CYAN_DARK = "\u001B[36m";
    public static final String GREEN = "\u001B[92m";
    public static final String YELLOW = "\u001B[93m";
    public static final String RED = "\u001B[91m";
    public static final String GRAY = "\u001B[90m";
    public static final String WHITE = "\u001B[97m";
    public static final String BOLD = "\u001B[1m";

    private static final String TEAL = CYAN;
    private static final int WIDTH = 46;

    public static void clearScreen() {
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        }
    }

    public static void title(String title) {
        String t = title == null ? "" : title.toUpperCase();
        int inner = Math.max(10, WIDTH - 2);
        int box = inner - 4;
        System.out.println(TEAL + "╔" + "═".repeat(inner) + "╗" + RESET);
        System.out.println(TEAL + "║ ┌" + "─".repeat(box) + "┐ ║" + RESET);
        System.out.println(TEAL + "║ │" + center(t, box) + TEAL + "│ ║" + RESET);
        System.out.println(TEAL + "║ └" + "─".repeat(box) + "┘ ║" + RESET);
        System.out.println(TEAL + "╚" + "═".repeat(inner) + "╝" + RESET);
    }

    /** Big block-letter banner shown on the welcome screen. */
    public static void banner(String name, String subtitle) {
        String[][] font = {
            glyph('C'), glyph('Y'), glyph('B'), glyph('E'), glyph('R'),
            glyph('A'), glyph('C'), glyph('C'), glyph('E'), glyph('S'), glyph('S')
        };
        String line = "═".repeat(70);
        System.out.println();
        System.out.println(TEAL + line + RESET);
        System.out.println();
        for (int r = 0; r < 5; r++) {
            StringBuilder b = new StringBuilder("  ");
            for (String[] g : font) b.append(g[r]).append("  ");
            System.out.println(TEAL + BOLD + b + RESET);
        }
        System.out.println();
        StringBuilder spaced = new StringBuilder();
        for (char c : subtitle.toUpperCase().toCharArray()) spaced.append(c).append(' ');
        System.out.println(WHITE + center(spaced.toString().trim().replace("  ", "   "), 70) + RESET);
        System.out.println();
        System.out.println(TEAL + line + RESET);
        System.out.println();
    }

    private static String[] glyph(char c) {
        String[] rows = switch (c) {
            case 'C' -> new String[]{"█████", "█    ", "█    ", "█    ", "█████"};
            case 'Y' -> new String[]{"█   █", " █ █ ", "  █  ", "  █  ", "  █  "};
            case 'B' -> new String[]{"████ ", "█   █", "████ ", "█   █", "████ "};
            case 'E' -> new String[]{"█████", "█    ", "████ ", "█    ", "█████"};
            case 'R' -> new String[]{"████ ", "█   █", "████ ", "█  █ ", "█   █"};
            case 'A' -> new String[]{" ███ ", "█   █", "█████", "█   █", "█   █"};
            case 'S' -> new String[]{"█████", "█    ", "█████", "    █", "█████"};
            default  -> new String[]{"     ", "     ", "     ", "     ", "     "};
        };
        return rows;
    }

    /** "Welcome, Name!" line shown under dashboard titles. */
    public static void welcome(String name) {
        System.out.println();
        System.out.println(CYAN + BOLD + "Welcome, " + name + "!" + RESET);
        System.out.println();
    }

    /** Heading used by screens that only need a title (forms, lists). */
    public static void heading(String text) {
        title(text);
        System.out.println();
    }

    public static void section(String text) {
        System.out.println();
        System.out.println(YELLOW + "── " + text + " ──" + RESET);
    }

    public static void menuItem(int number, String label) {
        menuItem(String.valueOf(number), label);
    }

    public static void menuItem(String number, String label) {
        String color = number.equals("0") ? RED : GREEN;
        System.out.println(color + "[" + number + "]" + RESET + " " + WHITE + label + RESET);
    }

    public static void divider() {
        System.out.println();
    }

    public static void prompt(String text) {
        System.out.print(CYAN_DARK + "» " + RESET + text);
    }

    public static void success(String text) {
        System.out.println(GREEN + " " + text + RESET);
    }

    public static void error(String text) {
        System.out.println(RED + " " + text + RESET);
    }

    public static void info(String text) {
        System.out.println(CYAN + " " + text + RESET);
    }

    public static void warning(String text) {
        System.out.println(YELLOW + " " + text + RESET);
    }

    public static void table(String[] headers, String[][] rows) {
        table(null, headers, rows);
    }

    /**
     * Boxed table: double outer border, centered title, single column
     * separators, and a blank spacer row under the header.
     */
    public static void table(String title, String[] headers, String[][] rows) {
        if (headers == null || headers.length == 0) return;
        int columns = headers.length;
        int[] widths = new int[columns];
        for (int i = 0; i < columns; i++) widths[i] = visibleLength(headers[i]);
        if (rows != null) {
            for (String[] row : rows) {
                for (int i = 0; i < columns; i++) {
                    String value = i < row.length && row[i] != null ? row[i] : "";
                    widths[i] = Math.max(widths[i], visibleLength(value));
                }
            }
        }
        for (int i = 0; i < columns; i++) widths[i] = Math.min(widths[i], 34);

        // inner width = columns' cells + separators between them
        int inner = columns - 1;
        for (int w : widths) inner += w + 2;
        String t = title == null ? "" : title.toUpperCase();
        int minInner = Math.max(t.length() + 2, 21); // title
        if (minInner > inner) {
            widths[columns - 1] += minInner - inner;
            inner = minInner;
        }

        System.out.println(CYAN_DARK + "╔" + "═".repeat(inner) + "╗" + RESET);
        if (!t.isEmpty()) {
            System.out.println(CYAN_DARK + "║" + RESET + center(t, inner) + CYAN_DARK + "║" + RESET);
            System.out.println(CYAN_DARK + "╠" + "═".repeat(inner) + "╣" + RESET);
        }
        System.out.println(CYAN_DARK + row(headers, widths, true) + RESET);
        System.out.println(CYAN_DARK + buildBorder(widths, '╠', '╪', '╣', '═') + RESET);
        if (rows == null || rows.length == 0) {
            System.out.println(CYAN_DARK + "║" + RESET + GRAY + padRight("  No records found.", inner) + RESET + CYAN_DARK + "║" + RESET);
        } else {
            System.out.println(CYAN_DARK + row(new String[0], widths, false) + RESET);
            for (String[] data : rows) System.out.println(row(data, widths, false));
        }
        System.out.println(CYAN_DARK + buildBorder(widths, '╚', '╧', '╝', '═') + RESET);
    }

    public static void empty(String text) {
        System.out.println(GRAY + "  " + text + RESET);
    }

    public static String money(double value) {
        return String.format("PHP %.2f", value);
    }

    private static String row(String[] values, int[] widths, boolean header) {
        String edge = CYAN_DARK + "║" + RESET;
        String sep = CYAN_DARK + "│" + RESET;
        String color = header ? CYAN_DARK : WHITE;
        StringBuilder b = new StringBuilder(edge);
        for (int i = 0; i < widths.length; i++) {
            String value = i < values.length && values[i] != null ? values[i] : "";
            if (visibleLength(value) > widths[i]) value = truncate(value, widths[i]);
            b.append(color).append(" ").append(padRight(value, widths[i])).append(" ").append(RESET);
            b.append(i == widths.length - 1 ? edge : sep);
        }
        return b.toString();
    }

    private static String buildBorder(int[] widths, char left, char junction, char right, char fill) {
        StringBuilder b = new StringBuilder().append(left);
        for (int i = 0; i < widths.length; i++) {
            b.append(String.valueOf(fill).repeat(widths[i] + 2));
            b.append(i == widths.length - 1 ? right : junction);
        }
        return b.toString();
    }

    private static String center(String value, int width) {
        if (value.length() >= width) return truncate(value, width);
        int left = (width - value.length()) / 2;
        int right = width - value.length() - left;
        return " ".repeat(left) + BOLD + value + RESET + " ".repeat(right);
    }

    private static String padRight(String value, int width) {
        return value + " ".repeat(Math.max(0, width - visibleLength(value)));
    }

    private static String truncate(String value, int width) {
        if (value.length() <= width) return value;
        return width <= 1 ? value.substring(0, width) : value.substring(0, width - 1) + "…";
    }

    private static int visibleLength(String value) {
        return value == null ? 0 : value.replaceAll("\\u001B\\[[;\\d]*m", "").length();
    }
}
