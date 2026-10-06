package com.minestorm.reportsystem.util;

import java.util.ArrayList;
import java.util.List;

public final class TextUtil {
    public static String clean(String input) {
        if (input == null) return "";
        return input.replace('\u00a7', ' ').replaceAll("[\\r\\n]+", " ");
    }

    public static List<String> wrap(String text, int width, int maxLines) {
        List<String> lines = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) continue;
            while (word.length() > width) {
                if (current.length() > 0) { lines.add(current.toString()); current.setLength(0); }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            int needed = current.length() + word.length() + (current.length() > 0 ? 1 : 0);
            if (needed > width) { lines.add(current.toString()); current.setLength(0); }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }
        if (current.length() > 0) lines.add(current.toString());
        if (lines.size() > maxLines) {
            lines = new ArrayList<String>(lines.subList(0, maxLines));
            lines.set(maxLines - 1, lines.get(maxLines - 1) + "...");
        }
        return lines;
    }
}
