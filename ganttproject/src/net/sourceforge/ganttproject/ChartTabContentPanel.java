/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.overview.NavigationPanel;
import net.sourceforge.ganttproject.chart.overview.ZoomingPanel;
import net.sourceforge.ganttproject.gui.GanttImagePanel;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

abstract class ChartTabContentPanel {
    private JSplitPane mySplitPane;
    private final List<Component> myPanels = new ArrayList<Component>();

    protected ChartTabContentPanel(IGanttProject project, UIFacade workbenchFacade, TimelineChart chart) {
        NavigationPanel navigationPanel = new NavigationPanel(project, chart, workbenchFacade);
        ZoomingPanel zoomingPanel = new ZoomingPanel(workbenchFacade, chart);
        addChartPanel(zoomingPanel.getComponent());
        addChartPanel(navigationPanel.getComponent());
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
        Dimension minSize = new Dimension(0, 0);
        left.setMinimumSize(minSize);

        JPanel right = new JPanel(new BorderLayout());
        right.add(createChartPanels(), BorderLayout.NORTH);
        right.setBackground(new Color(0.93f, 0.93f, 0.93f));
        right.add(getChartComponent(), BorderLayout.CENTER);
        right.setMinimumSize(minSize);

        mySplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        if (GanttLanguage.getInstance().getComponentOrientation() == ComponentOrientation.LEFT_TO_RIGHT) {
            mySplitPane.setLeftComponent(left);
            mySplitPane.setRightComponent(right);
            mySplitPane.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            mySplitPane.setDividerLocation((int) left.getPreferredSize().getWidth());
        } else {
            mySplitPane.setRightComponent(left);
            mySplitPane.setLeftComponent(right);
            mySplitPane.setDividerLocation(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - left.getPreferredSize().getWidth()));
            mySplitPane.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        }
        mySplitPane.setOneTouchExpandable(true);
        mySplitPane.resetToPreferredSizes();
        tabContentPanel.add(mySplitPane, BorderLayout.CENTER);
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
        for (Component panel : myPanels) {
            panelsBox.add(panel);
            panelsBox.add(Box.createHorizontalStrut(10));
        }
        result.add(panelsBox, BorderLayout.WEST);
        result.setBackground(new Color(0.93f, 0.93f, 0.93f));

        return result;
    }

    protected void addChartPanel(Component panel) {
        myPanels.add(panel);
    }
}