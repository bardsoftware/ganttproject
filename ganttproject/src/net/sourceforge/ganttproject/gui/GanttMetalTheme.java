package net.sourceforge.ganttproject.gui;

//import java.awt.Font;

import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * Un nouveau thème Metal.
 */

public class GanttMetalTheme extends DefaultMetalTheme {
    // cette fonte sera utilisée pour TOUS nos composants
    private final FontUIResource font;

    public GanttMetalTheme(UIConfiguration config) {
        super();
        font = new FontUIResource(config.getMenuFont());
    }

    public FontUIResource getControlTextFont() {
        return font;
    }

    public FontUIResource getMenuTextFont() {
        return font;
    }

    public FontUIResource getSystemTextFont() {
        return font;
    }

    public FontUIResource getUserTextFont() {
        return font;
    }

    public FontUIResource getWindowTitleFont() {
        return font;
    }
}
