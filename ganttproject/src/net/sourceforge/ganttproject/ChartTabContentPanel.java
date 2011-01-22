package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.overview.NavigationPanel;
import net.sourceforge.ganttproject.chart.overview.ZoomingPanel;
import net.sourceforge.ganttproject.gui.GanttImagePanel;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

abstract class ChartTabContentPanel {

    private JSplitPane mySplitPane;
    protected final NavigationPanel myNavigationPanel;
    protected final ZoomingPanel myZoomingPanel;

    protected ChartTabContentPanel(IGanttProject project, UIFacade workbenchFacade, TimelineChart chart) {
        myNavigationPanel = new NavigationPanel(project, chart, workbenchFacade);
        myZoomingPanel = new ZoomingPanel(workbenchFacade, chart);
    }
    protected JComponent createContentComponent() {
        JPanel tabContentPanel = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new BorderLayout());
        Box treeHeader = Box.createVerticalBox();
        Component buttonPanel = createButtonPanel();
        treeHeader.add(buttonPanel);
        treeHeader.add(new GanttImagePanel(AbstractChartImplementation.LOGO, 300, AbstractChartImplementation.LOGO.getIconHeight()));
        left.add(treeHeader, BorderLayout.NORTH);

        left.add(getTreeComponent(), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.add(createChartPanels(), BorderLayout.NORTH);
        right.setBackground(new Color(0.93f, 0.93f, 0.93f));
        right.add(getChartComponent(), BorderLayout.CENTER);

        // A splitpane is used
        mySplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        if (GanttLanguage.getInstance().getComponentOrientation() == ComponentOrientation.LEFT_TO_RIGHT) {
            mySplitPane.setLeftComponent(left);
            mySplitPane.setRightComponent(right);
            mySplitPane.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            mySplitPane.setDividerLocation((int) left.getPreferredSize().getWidth());
        } else {
            mySplitPane.setRightComponent(left);
            mySplitPane.setLeftComponent(right);
            mySplitPane.setDividerLocation((int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - left
                    .getPreferredSize().getWidth()));
            mySplitPane.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        }
        mySplitPane.setOneTouchExpandable(true);
        mySplitPane.setPreferredSize(new Dimension(800, 500));
        tabContentPanel.add(mySplitPane, BorderLayout.CENTER);

        tabContentPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(GPAction.getKeyStroke("overview.shortcut"), "overview");

        return tabContentPanel;
    }

    protected abstract Component getChartComponent();

    protected abstract Component getTreeComponent();

    protected abstract Component createButtonPanel();

    protected int getDividerLocation() {
        return mySplitPane.getDividerLocation();
    }

    protected void setDividerLocation(int location) {
        mySplitPane.setDividerLocation(location);
    }

    private Component createChartPanels() {
        JPanel result = new JPanel(new BorderLayout());

        Box panelsBox = Box.createHorizontalBox();
        panelsBox.add(createZoomingPanel());
        panelsBox.add(Box.createHorizontalStrut(10));
        panelsBox.add(createNavigationPanel());
        result.add(panelsBox, BorderLayout.WEST);
        result.setBackground(new Color(0.93f, 0.93f, 0.93f));
        
        return result;
    }

    private Component createZoomingPanel() {
        return myZoomingPanel.getComponent();
    }

    private Component createNavigationPanel() {
        return myNavigationPanel.getComponent();
    }
}