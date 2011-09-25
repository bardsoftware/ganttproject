package net.sourceforge.ganttproject.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

public abstract class UIUtil {
    public static void repackWindow(JComponent component) {
        Window windowAncestor = SwingUtilities.getWindowAncestor(component);
        if (windowAncestor != null) {
            windowAncestor.invalidate();
            windowAncestor.pack();
            windowAncestor.repaint();
        }
        DialogAligner.centerOnScreen(windowAncestor);
    }

    public static void setEnabledTree(JComponent root, boolean isEnabled) {
        root.setEnabled(isEnabled);
        Component[] components = root.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JComponent) {
                setEnabledTree((JComponent) components[i], isEnabled);
            }
        }
    }

    public static void setBackgroundTree(JComponent root, Color background) {
        root.setBackground(background);
        Component[] components = root.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JComponent) {
                setBackgroundTree((JComponent) components[i], background);
            }
        }
    }

    public static void createTitle(JComponent component, String title) {
        Border lineBorder = BorderFactory.createMatteBorder(1,0,0,0,Color.BLACK);
        component.setBorder(BorderFactory.createTitledBorder(lineBorder, title));
    }

}
