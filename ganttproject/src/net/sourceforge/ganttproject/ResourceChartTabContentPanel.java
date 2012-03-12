/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.view.GPView;

class ResourceChartTabContentPanel extends ChartTabContentPanel implements GPView {
  private ResourceTreeUIFacade myTreeFacade;
  private Component myResourceChart;
  private JComponent myTabContentPanel;

  ResourceChartTabContentPanel(IGanttProject project, UIFacade workbenchFacade, ResourceTreeUIFacade resourceTree,
      Component resourceChart) {
    super(project, workbenchFacade, (TimelineChart) workbenchFacade.getResourceChart());
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
    JToolBar buttonBar = new JToolBar();
    buttonBar.setFloatable(false);
    buttonBar.setBorderPainted(false);

    TestGanttRolloverButton upButton = new TestGanttRolloverButton(myTreeFacade.getMoveUpAction());
    upButton.setTextHidden(true);
    buttonBar.add(upButton);

    TestGanttRolloverButton downButton = new TestGanttRolloverButton(myTreeFacade.getMoveDownAction());
    downButton.setTextHidden(true);
    buttonBar.add(downButton);

    JPanel buttonPanel = new JPanel(new BorderLayout());
    buttonPanel.add(buttonBar, BorderLayout.WEST);
    return buttonPanel;
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