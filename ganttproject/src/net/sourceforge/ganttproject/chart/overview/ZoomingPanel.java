package net.sourceforge.ganttproject.chart.overview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import net.sourceforge.ganttproject.action.ZoomInAction;
import net.sourceforge.ganttproject.action.ZoomOutAction;
import net.sourceforge.ganttproject.gui.UIFacade;

public class ZoomingPanel {
    private final ZoomInAction myZoomInAction;
    private final ZoomOutAction myZoomOutAction;

    public ZoomingPanel(UIFacade workbenchFacade) {
        myZoomInAction = new ZoomInAction(workbenchFacade.getZoomManager(), "16");
        myZoomOutAction = new ZoomOutAction(workbenchFacade.getZoomManager(), "16");
    }

    public Component getComponent() {
        JToolBar buttonBar = new JToolBar();
        buttonBar.setBackground(new Color(0.93f, 0.93f, 0.93f));
        buttonBar.setFloatable(false);
        buttonBar.setBorderPainted(false);
        final JButton zoomIn = new JButton("<html><b>&nbsp;Zoom In&nbsp;</b></html>");
        zoomIn.setBorderPainted(false);
        zoomIn.setMargin(new Insets(0, 0, 0, 0));
        zoomIn.setIcon(new ImageIcon(getClass().getResource("/icons/blank_big.gif")));
        zoomIn.setHorizontalTextPosition(SwingConstants.CENTER);
        zoomIn.setVerticalTextPosition(SwingConstants.CENTER);
        zoomIn.addMouseListener(new HighlightOnMouseOver(zoomIn, null));
        zoomIn.setRolloverEnabled(true);
        zoomIn.setBorderPainted(false);
        zoomIn.addActionListener(myZoomInAction);
        buttonBar.add(zoomIn);
        buttonBar.add(new JLabel(" | "));
        JButton zoomOut = new JButton("<html><b>&nbsp;Zoom Out&nbsp;</b></html>");
        zoomOut.setMargin(new Insets(0, 0, 0, 0));
        zoomOut.setRolloverEnabled(true);
        zoomOut.setBorderPainted(false);
        zoomOut.addActionListener(myZoomOutAction);
        zoomOut.addMouseListener(new HighlightOnMouseOver(zoomOut, null));
        buttonBar.add(zoomOut);
        
        return buttonBar;
    }
}