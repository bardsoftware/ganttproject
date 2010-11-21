package net.sourceforge.ganttproject.gui;

import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * A new Metal theme. Providing one font for all components.
 */
public class GanttMetalTheme extends DefaultMetalTheme {
    // This font will be used for ALL of our components
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
