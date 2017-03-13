/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
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

import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.overview.NavigationPanel;
import net.sourceforge.ganttproject.chart.overview.ZoomingPanel;
import net.sourceforge.ganttproject.gui.GanttImagePanel;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

abstract class ChartTabContentPanel {
  private JSplitPane mySplitPane;
  private final List<Component> myPanels = new ArrayList<Component>();
  private final UIFacade myUiFacade;

  protected ChartTabContentPanel(IGanttProject project, UIFacade workbenchFacade, TimelineChart chart) {
    NavigationPanel navigationPanel = new NavigationPanel(project, chart, workbenchFacade);
    ZoomingPanel zoomingPanel = new ZoomingPanel(workbenchFacade, chart);
    addChartPanel(zoomingPanel.getComponent());
    addChartPanel(navigationPanel.getComponent());
    myUiFacade = workbenchFacade;
  }

  protected JComponent createContentComponent() {
    JPanel tabContentPanel = new JPanel(new BorderLayout());
    final JPanel left = new JPanel(new BorderLayout());
    Box treeHeader = Box.createVerticalBox();
    final JComponent buttonPanel = (JComponent) createButtonPanel();
    JPanel buttonWrapper = new JPanel(new BorderLayout());
    buttonWrapper.add(buttonPanel, BorderLayout.WEST);
    //button.setAlignmentX(Component.LEFT_ALIGNMENT);
    treeHeader.add(buttonWrapper);

    treeHeader.add(new GanttImagePanel(myUiFacade.getLogo(), 300, myUiFacade.getLogo().getHeight(null)));
    left.add(treeHeader, BorderLayout.NORTH);

    left.add(getTreeComponent(), BorderLayout.CENTER);
    Dimension minSize = new Dimension(0, 0);
    left.setMinimumSize(minSize);

    JPanel right = new JPanel(new BorderLayout());
    final JComponent chartPanels = createChartPanels();
    right.add(chartPanels, BorderLayout.NORTH);
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
      mySplitPane.setDividerLocation((int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - left.getPreferredSize().getWidth()));
      mySplitPane.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }
    mySplitPane.setOneTouchExpandable(true);
    mySplitPane.resetToPreferredSizes();
    tabContentPanel.add(mySplitPane, BorderLayout.CENTER);

    ChangeValueListener changeValueListener = new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            alignTopPanelHeights(buttonPanel, chartPanels);
          }
        });
      }
    };
    myUiFacade.getDpiOption().addChangeValueListener(changeValueListener, 2);
    return tabContentPanel;
  }

  private void alignTopPanelHeights(JComponent buttonPanel, JComponent chartPanels) {
    int maxHeight = Math.max(buttonPanel.getSize().height, chartPanels.getSize().height);
    if (buttonPanel.getHeight() < maxHeight) {
      //left.setBorder(BorderFactory.createEmptyBorder(maxHeight - buttonPanel.getHeight(), 0, 0, 0));
      int diff = maxHeight - buttonPanel.getHeight();
      Border emptyBorder = BorderFactory.createEmptyBorder((diff+1)/2, 0, diff/2, 0);
      buttonPanel.setBorder(emptyBorder);
    }
    if (chartPanels.getHeight() < maxHeight) {
      int diff = maxHeight - chartPanels.getHeight();
      //Border emptyBorder = BorderFactory.createEmptyBorder((diff+1)/2, 0, diff/2, 0);
      //chartPanels.setBorder(emptyBorder);
      chartPanels.remove(chartPanels.getComponent(chartPanels.getComponentCount() - 1));
      chartPanels.add(Box.createRigidArea(new Dimension(0, diff)));
    }
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

  private JComponent createChartPanels() {
    Box panelsBox = Box.createHorizontalBox();
    for (Component panel : myPanels) {
      panelsBox.add(panel);
      panelsBox.add(Box.createHorizontalStrut(10));
    }
    return panelsBox;
  }

  protected void addChartPanel(Component panel) {
    myPanels.add(panel);
  }

  protected UIFacade getUiFacade() {
    return myUiFacade;
  }
}