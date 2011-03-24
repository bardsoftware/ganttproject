package net.sourceforge.ganttproject.parser;

import java.awt.Color;
import java.util.regex.Pattern;

import net.sourceforge.ganttproject.GanttGraphicArea;

class ColorValueParser {
    public static Color parseString(String value) {
        if (!Pattern.matches("#[0-9abcdefABCDEF]{6}+", value)) {
            return GanttGraphicArea.taskDefaultColor;
        }
        int r, g, b;
        r = Integer.valueOf(value.substring(1, 3), 16).intValue();
        g = Integer.valueOf(value.substring(3, 5), 16).intValue();
        b = Integer.valueOf(value.substring(5, 7), 16).intValue();
        return new Color(r, g, b);
    }
}
