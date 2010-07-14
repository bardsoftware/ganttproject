package net.sourceforge.ganttproject.gui;

import java.awt.Component;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

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

}
