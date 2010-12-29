package net.sourceforge.ganttproject.chart.overview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

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
        //final Box buttonBar = Box.createHorizontalBox();
        //final JPanel buttonBar = new JPanel(new GridLayout(1, 3));
        //buttonBar.setBackground(Color.DARK_GRAY.brighter());
        JToolBar buttonBar = new JToolBar();
        buttonBar.setBackground(new Color(0.93f, 0.93f, 0.93f));
        buttonBar.setFloatable(false);
        buttonBar.add(Box.createHorizontalStrut(20));
        final JButton zoomIn = new JButton("<html><b>&nbsp;Zoom In&nbsp;</b></html>");
        zoomIn.setBorderPainted(false);
        zoomIn.setMargin(new Insets(0, 0, 0, 0));
//        zoomIn.setBorder(BorderFactory.createCompoundBorder(
//            BorderFactory.createEmptyBorder(3, 0, 3, 0), 
//            BorderFactory.createMatteBorder(0, 3, 0, 0, zoomIn.getForeground())));
        zoomIn.addMouseListener(new HighlightOnMouseOver(zoomIn, null));
        zoomIn.setRolloverEnabled(true);
        zoomIn.setBorderPainted(false);
        zoomIn.addActionListener(myZoomInAction);
        //zoomIn.setBorder(new RoundedBorder(5));
        buttonBar.add(zoomIn);
        buttonBar.add(new JLabel(" | "));
        JButton zoomOut = new JButton("<html><b>&nbsp;Zoom Out&nbsp;</b></html>");
        zoomOut.setMargin(new Insets(0, 0, 0, 0));
        zoomOut.setRolloverEnabled(true);
        zoomOut.setBorderPainted(false);
        zoomOut.addActionListener(myZoomOutAction);
//        zoomOut.setBorder(BorderFactory.createCompoundBorder(
//            BorderFactory.createEmptyBorder(3, 0, 3, 0),
//            BorderFactory.createMatteBorder(0, 0, 0, 3, zoomOut.getForeground())));
        zoomOut.addMouseListener(new HighlightOnMouseOver(zoomOut, null));
        buttonBar.add(zoomOut);
        
        //buttonStrip.setPreferredSize(new Dimension(100, 20));
        return buttonBar;
    }

}