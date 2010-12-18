package net.sourceforge.ganttproject.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JFrame;

public class DialogAligner {
    public static void center(JDialog dialog, Container parent) {
        boolean alignToParent = false;
        if (parent != null) {
            alignToParent = parent.isVisible();
        }

        if (alignToParent) {
            Point point = parent.getLocationOnScreen();
            int x = (int) point.getX() + parent.getWidth() / 2;
            int y = (int) point.getY() + parent.getHeight() / 2;
            dialog.setLocation(x - dialog.getWidth() / 2, y
                    - dialog.getHeight() / 2);
        } else {
            centerOnScreen(dialog);
        }
    }

    public static void center(JFrame frame) {
        centerOnScreen(frame);
    }

    public static void centerOnScreen(Component component) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        component.setLocation(
                screenSize.width / 2 - (component.getWidth() / 2),
                screenSize.height / 2 - (component.getHeight() / 2));
    }
}
