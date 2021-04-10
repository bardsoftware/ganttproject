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
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.overview.NavigationPanel;
import net.sourceforge.ganttproject.chart.overview.ZoomingPanel;
import net.sourceforge.ganttproject.gui.GanttImagePanel;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

abstract class ChartTabContentPanel {
  private final TimelineChart myChart;
  private JSplitPane mySplitPane;
  private final List<Component> myPanels = new ArrayList<>();
  private final UIFacade myUiFacade;
  private int myImageHeight;
  private Supplier<Integer> myHeaderHeight;
  private GanttImagePanel myImagePanel;

  ChartTabContentPanel(IGanttProject project, UIFacade workbenchFacade, TimelineChart chart) {
    NavigationPanel navigationPanel = new NavigationPanel(project, chart, workbenchFacade);
    ZoomingPanel zoomingPanel = new ZoomingPanel(workbenchFacade, chart);
    addChartPanel(zoomingPanel.getComponent());
    addChartPanel(navigationPanel.getComponent());
    myUiFacade = workbenchFacade;
    myChart = Preconditions.checkNotNull(chart);
    myUiFacade.getMainFrame().addWindowListener(new WindowAdapter() {
      @Override
      public void windowOpened(WindowEvent windowEvent) {
        updateTimelineHeight();
      }
    });
  }

  JComponent createContentComponent() {
    JPanel tabContentPanel = new JPanel(new BorderLayout());
    final JPanel left = new JPanel(new BorderLayout());
    final Box treeHeader = Box.createVerticalBox();
    final JComponent buttonPanel = (JComponent) createButtonPanel();
    JPanel buttonWrapper = new JPanel(new BorderLayout());
    buttonWrapper.add(buttonPanel, BorderLayout.WEST);
    //button.setAlignmentX(Component.LEFT_ALIGNMENT);
    treeHeader.add(buttonWrapper);

    int defaultScaledHeight = (int)(UIFacade.DEFAULT_LOGO.getIconHeight() * myUiFacade.getDpiOption().getValue() / (1f * UIFacade.DEFAULT_DPI));
    myImagePanel = new GanttImagePanel(myUiFacade.getLogo(), 300, defaultScaledHeight);
    myImageHeight = myImagePanel.getPreferredSize().height;
    JPanel imageWrapper = new JPanel(new BorderLayout());
    imageWrapper.add(myImagePanel, BorderLayout.WEST);
    //myImagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    treeHeader.add(imageWrapper);

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
      mySplitPane.setDividerLocation(Math.min(300, left.getPreferredSize().width));
    } else {
      mySplitPane.setRightComponent(left);
      mySplitPane.setLeftComponent(right);
      mySplitPane.setDividerLocation(
          Toolkit.getDefaultToolkit().getScreenSize().width - Math.min(300, left.getPreferredSize().width));
      mySplitPane.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }
    mySplitPane.setOneTouchExpandable(true);
    mySplitPane.resetToPreferredSizes();
    tabContentPanel.add(mySplitPane, BorderLayout.CENTER);

    ChangeValueListener changeValueListener = new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        if (myUiFacade.getDpiOption().getValue() < 96) {
          return;
        }
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            alignTopPanelHeights(buttonPanel, chartPanels);
            myImagePanel.setScale(myUiFacade.getDpiOption().getValue() / (1f * UIFacade.DEFAULT_DPI));
            myImageHeight = myImagePanel.getHeight();
            updateTimelineHeight();
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

  int getDividerLocation() {
    return mySplitPane.getDividerLocation();
  }

  void setDividerLocation(int location) {
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

  void addChartPanel(Component panel) {
    myPanels.add(panel);
  }

  protected UIFacade getUiFacade() {
    return myUiFacade;
  }

  private void updateTimelineHeight() {
    int timelineHeight = myHeaderHeight.get() + myImageHeight;
    myChart.setTimelineHeight(timelineHeight);
  }

  void addTableResizeListeners(final Component tableContainer, final Component table) {
    myHeaderHeight = new Supplier<Integer>() {
      @Override
      public Integer get() {
        if (table.isShowing() && tableContainer.isShowing()) {
          Point tableLocation = table.getLocationOnScreen();
          Point containerLocation = tableContainer.getLocationOnScreen();
          return tableLocation.y - containerLocation.y;
        } else {
          return 0;
        }
      }
    };
    ComponentAdapter componentListener = new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent componentEvent) {
        updateTimelineHeight();
      }

      @Override
      public void componentResized(ComponentEvent componentEvent) {
        updateTimelineHeight();
      }

      @Override
      public void componentMoved(ComponentEvent componentEvent) {
        updateTimelineHeight();
      }
    };
    tableContainer.addComponentListener(componentListener);
    table.addComponentListener(componentListener);
  }

  public void setActive(boolean active) {
    if (active) {
      getTreeComponent().requestFocus();
      updateTimelineHeight();
    }
  }
}