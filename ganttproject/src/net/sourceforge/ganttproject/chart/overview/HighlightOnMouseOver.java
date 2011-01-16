/**
 *
 */
package net.sourceforge.ganttproject.chart.overview;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.UIManager;

class HighlightOnMouseOver extends MouseAdapter {
    static final Color backgroundColor = UIManager.getColor("MenuItem.selectionBackground");
    static final String backgroundString = "#" +
        Integer.toHexString(backgroundColor.getRed()) +
        Integer.toHexString(backgroundColor.getGreen()) +
        Integer.toHexString(backgroundColor.getBlue());
    private AbstractButton myComponent;
    private Color myColorNoMouse;
    private Action myActionOnClick;
    private Color myNormalBackground;
    private Color myNormalForeground;

    HighlightOnMouseOver(AbstractButton component, Action onClick) {
        myComponent = component;
        myActionOnClick = onClick;
    }
    @Override
    public void mouseEntered(MouseEvent arg0) {
        if (myComponent.isEnabled()) {
            myComponent.setBorderPainted(true);
        }
    }
    @Override
    public void mouseExited(MouseEvent arg0) {
        myComponent.setBorderPainted(false);        
    }
    @Override
    public void mouseClicked(MouseEvent arg0) {
        if (myActionOnClick!=null) {
            myActionOnClick.actionPerformed(null);
        }
    }
}