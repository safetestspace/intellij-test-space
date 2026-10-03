package com.github.safetestspace.testspace.formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TestNameFormatter {
    private static final Pattern CAMEL_BOUNDARY = Pattern.compile(
            "(?<=[\\p{Ll}\\p{Nd}])(?=\\p{Lu})");

    private record WordRange(int start, int end, char initial) {
        boolean containsBoundary(int offset) {
            return start < offset && offset < end;
        }
    }

    private TestNameFormatter() {
    }

    public static String format(String name, boolean replaceUnderscores, boolean splitCamelCase) {
        return format(name, replaceUnderscores, splitCamelCase, Set.of());
    }

    /**
     * Lowercases split words, preserving acronyms and restoring protected names' case.
     * Protected names match with either case for their first letter.
     */
    public static String format(String name, boolean replaceUnderscores, boolean splitCamelCase,
                                Set<String> protectedWords) {
        String result = replaceUnderscores ? name.replace('_', ' ') : name;
        if (!splitCamelCase) {
            return result;
        }
        List<WordRange> protectedRanges = findProtectedRanges(name, protectedWords);
        StringBuilder stringBuilder = new StringBuilder(result.length() + 8);
        int copied = 0;
        Matcher matcher = CAMEL_BOUNDARY.matcher(result);
        while (matcher.find()) {
            int boundary = matcher.start();
            if (isInside(boundary, protectedRanges)) {
                continue;
            }
            stringBuilder.append(result, copied, boundary).append(' ');
            copied = boundary;
            WordRange protectedRange = rangeStartingAt(boundary, protectedRanges);
            if (protectedRange != null) {
                stringBuilder.append(protectedRange.initial());
                copied = boundary + 1;
            } else if (isCapitalizedWord(result, boundary)) {
                stringBuilder.append(Character.toLowerCase(result.charAt(boundary)));
                copied = boundary + 1;
            }
        }
        return stringBuilder.append(result, copied, result.length()).toString();
    }

    /**
     * Whole-word matches as [start, end) ranges.
     */
    private static List<WordRange> findProtectedRanges(String name, Set<String> protectedWords) {
        List<WordRange> ranges = new ArrayList<>();
        for (String word : protectedWords) {
            if (word.length() < 2) {
                continue;
            }
            for (int start = 0; start + word.length() <= name.length(); start++) {
                int end = start + word.length();
                if (matchesAt(name, word, start) && startsWord(name, start) && endsWord(name, end)) {
                    ranges.add(new WordRange(start, end, word.charAt(0)));
                }
            }
        }
        return ranges;
    }

    private static boolean matchesAt(String name, String word, int start) {
        return Character.toLowerCase(name.charAt(start)) == Character.toLowerCase(word.charAt(0))
                && name.startsWith(word.substring(1), start + 1);
    }

    private static boolean startsWord(String name, int start) {
        return start == 0 || !Character.isLetterOrDigit(name.charAt(start - 1))
                || Character.isUpperCase(name.charAt(start));
    }

    private static boolean endsWord(String name, int end) {
        return end == name.length() || !Character.isLowerCase(name.charAt(end));
    }

    private static boolean isCapitalizedWord(String text, int start) {
        return start + 1 < text.length()
                && Character.isUpperCase(text.charAt(start)) && Character.isLowerCase(text.charAt(start + 1));
    }

    private static WordRange rangeStartingAt(int boundary, List<WordRange> ranges) {
        for (WordRange range : ranges) {
            if (boundary == range.start()) {
                return range;
            }
        }
        return null;
    }

    private static boolean isInside(int boundary, List<WordRange> ranges) {
        for (WordRange range : ranges) {
            if (range.containsBoundary(boundary)) {
                return true;
            }
        }
        return false;
    }
}
