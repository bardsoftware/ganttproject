package net.sourceforge.ganttproject.chart.overview;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;

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
        final Box buttonBar = Box.createHorizontalBox();
        //final JPanel buttonBar = new JPanel(new GridLayout(1, 3));
        //buttonBar.setBackground(Color.DARK_GRAY.brighter());
        buttonBar.add(Box.createHorizontalStrut(20));
        JButton zoomIn = new JButton("<html><b>&nbsp;Zoom In&nbsp;</b></html>");
        zoomIn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(3, 0, 3, 0), 
            BorderFactory.createMatteBorder(0, 3, 0, 1, Color.BLACK)));
        zoomIn.addMouseListener(new HighlightOnMouseOver(zoomIn, zoomIn.getBackground(), null));
        zoomIn.setRolloverEnabled(true);
        zoomIn.addActionListener(myZoomInAction);
        //zoomIn.setBorder(new RoundedBorder(5));
        buttonBar.add(zoomIn);
                
        JButton zoomOut = new JButton("<html><b>&nbsp;Zoom Out&nbsp;</b></html>");
        zoomOut.addActionListener(myZoomOutAction);
        zoomOut.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(3, 0, 3, 0),
            BorderFactory.createMatteBorder(0, 0, 0, 3, Color.BLACK)));
        buttonBar.add(zoomOut);
        
        //buttonStrip.setPreferredSize(new Dimension(100, 20));
        return buttonBar;
    }

}