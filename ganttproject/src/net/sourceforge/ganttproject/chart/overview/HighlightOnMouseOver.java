/**
 *
 */
package net.sourceforge.ganttproject.chart.overview;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.UIManager;

class HighlightOnMouseOver extends MouseAdapter {
    static final Color backgroundColor = UIManager.getColor("MenuItem.selectionBackground");
    static final String backgroundString = "#" +
        Integer.toHexString(backgroundColor.getRed()) +
        Integer.toHexString(backgroundColor.getGreen()) +
        Integer.toHexString(backgroundColor.getBlue());
    private JComponent myComponent;
    private Color myColorNoMouse;
    private Action myActionOnClick;

    HighlightOnMouseOver(JComponent component, Color colNoMouse, Action onClick) {
        myComponent = component;
        myColorNoMouse = colNoMouse;
        myComponent.setOpaque(true);
        myActionOnClick = onClick;
    }
    public void mouseEntered(MouseEvent arg0) {
        myComponent.setBackground(getSelectionBackground());
        myComponent.setForeground(Color.WHITE);
    }
    @Override
    public void mouseExited(MouseEvent arg0) {
        myComponent.setBackground(myColorNoMouse);
        myComponent.setForeground(Color.BLACK);
    }
    @Override
    public void mouseClicked(MouseEvent arg0) {
        if (myActionOnClick!=null) {
            myActionOnClick.actionPerformed(null);
        }
    }

    static Color getSelectionBackground() {
        return UIManager.getColor("MenuItem.selectionBackground");
    }
}