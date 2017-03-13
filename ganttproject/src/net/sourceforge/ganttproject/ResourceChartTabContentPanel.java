/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.overview.GPToolbar;
import net.sourceforge.ganttproject.chart.overview.ToolbarBuilder;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.view.GPView;

import javax.swing.*;
import java.awt.*;

class ResourceChartTabContentPanel extends ChartTabContentPanel implements GPView {
  private ResourceTreeUIFacade myTreeFacade;
  private Component myResourceChart;
  private JComponent myTabContentPanel;

  ResourceChartTabContentPanel(IGanttProject project, UIFacade workbenchFacade, ResourceTreeUIFacade resourceTree,
      Component resourceChart) {
    super(project, workbenchFacade, workbenchFacade.getResourceChart());
    myTreeFacade = resourceTree;
    myResourceChart = resourceChart;
  }

  JComponent getComponent() {
    if (myTabContentPanel == null) {
      myTabContentPanel = createContentComponent();
    }
    return myTabContentPanel;
  }

  @Override
  protected Component createButtonPanel() {
    ToolbarBuilder builder = new ToolbarBuilder()
        .withHeight(24)
        .withSquareButtons()
        .withDpiOption(getUiFacade().getDpiOption())
        .addButton(myTreeFacade.getMoveUpAction())
        .addButton(myTreeFacade.getMoveDownAction());
    final GPToolbar toolbar = builder.build();
    return toolbar.getToolbar();
  }

  @Override
  protected Component getChartComponent() {
    return myResourceChart;
  }

  @Override
  protected Component getTreeComponent() {
    return myTreeFacade.getTreeComponent();
  }

  @Override
  public void setActive(boolean active) {
    if (active) {
      getTreeComponent().requestFocus();
    }
  }

  @Override
  public Chart getChart() {
    return getUiFacade().getResourceChart();
  }

  @Override
  public Component getViewComponent() {
    return getComponent();
  }
}