package com.minestorm.reportsystem.util;

import java.util.ArrayList;
import java.util.List;

public final class TextUtil {
    public static String clean(String i) {
        if (i == null) return "";
        return i.replace('\u00a7', ' ').replaceAll("[\\r\\n]+", " ");
    }
    public static List<String> wrap(String text, int width, int max) {
        List<String> lines = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            if (w.isEmpty()) continue;
            while (w.length() > width) {
                if (cur.length() > 0) { lines.add(cur.toString()); cur.setLength(0); }
                lines.add(w.substring(0, width));
                w = w.substring(width);
            }
            int need = cur.length() + w.length() + (cur.length() > 0 ? 1 : 0);
            if (need > width) { lines.add(cur.toString()); cur.setLength(0); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        if (lines.size() > max) {
            lines = new ArrayList<String>(lines.subList(0, max));
            lines.set(max - 1, lines.get(max - 1) + "...");
        }
        return lines;
    }
}
