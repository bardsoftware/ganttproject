package net.sourceforge.ganttproject.chart.overview;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

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
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 20, 0, 0),
                new LineBorder(HighlightOnMouseOver.backgroundColor, 1));
        buttonBar.setBorder(border);

        buttonBar.add(new PanelBorder());
        final JLabel zoomIn = new JLabel("<html><b>&nbsp;Zoom In&nbsp;</b></html>");
        zoomIn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        buttonBar.add(zoomIn);
        zoomIn.addMouseListener(new HighlightOnMouseOver(zoomIn, buttonBar.getBackground(), myZoomInAction));

        buttonBar.add(new JLabel(" | "));

        final JLabel zoomOut = new JLabel("<html><b>&nbsp;Zoom Out&nbsp;</b></html>");
        zoomOut.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        buttonBar.add(zoomOut);
        zoomOut.addMouseListener(new HighlightOnMouseOver(zoomOut, buttonBar.getBackground(), myZoomOutAction));

        buttonBar.add(new PanelBorder());
        return buttonBar;
    }

}