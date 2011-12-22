package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.ganttproject.util.TextLengthCalculatorImpl;

public class FontChooser {

    private Properties myProperties;
    private Map<String, Font> myFonts = new HashMap<String, Font>();
    private TextLengthCalculatorImpl myCalculator;

    public FontChooser(Properties properties, TextLengthCalculatorImpl calculator) {
        myProperties = properties;
        myCalculator = calculator;
    }

    public void decreaseBaseFontSize() {
        Map<String, Font> newFonts = new HashMap<String, Font>();
        for (String style : myFonts.keySet()) {
            Font f = myFonts.get(style);
            f = f.deriveFont(f.getSize() - 1f);
            newFonts.put(style, f);
        }
        myFonts = newFonts;
    }

    public int getMarginTop(String style) {
        return Integer.parseInt(myProperties.getProperty(style + ".margin-top", "0"));
    }

    public int getTextHeight(String style) {
        Font f = getFont(style);
        return myCalculator.getTextHeight(f, "A");
    }

    public int getMarginBottom(String style) {
        return Integer.parseInt(myProperties.getProperty(style + ".margin-bottom", "0"));
    }

    public Font getFont(String style) {
        Font f = myFonts.get(style);
        if (f == null) {
            f = Font.decode(myProperties.getProperty(style + ".font", "Dialog 10"));
            myFonts.put(style, f);
        }
        return f;
    }

    public Color getColor(String style) {
        return Color.decode(myProperties.getProperty(style + ".color", "#000"));
    }
}
