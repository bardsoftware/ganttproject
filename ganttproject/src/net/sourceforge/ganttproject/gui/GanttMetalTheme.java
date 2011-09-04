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

    @Override
    public FontUIResource getControlTextFont() {
        return font;
    }

    @Override
    public FontUIResource getMenuTextFont() {
        return font;
    }

    @Override
    public FontUIResource getSystemTextFont() {
        return font;
    }

    @Override
    public FontUIResource getUserTextFont() {
        return font;
    }

    @Override
    public FontUIResource getWindowTitleFont() {
        return font;
    }
}
