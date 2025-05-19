/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 Thomas Alexandre, GanttProject Team

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

import biz.ganttproject.ganttview.ResourceTableChartConnector;
import javafx.beans.value.ChangeListener;
import kotlin.Unit;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.resource.ResourceActionSet;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.overview.ToolbarBuilder;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.*;
import net.sourceforge.ganttproject.util.collect.Pair;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class GanttResourcePanel extends TreeTableContainer<HumanResource, ResourceTreeTable, ResourceTreeTableModel>
    implements ResourceTreeUIFacade {

  public final GanttProject appli;

  //private final ResourceActionSet myResourceActionSet;

//  private final GanttProjectBase.RowHeightAligner myRowHeightAligner;

  public ResourceLoadGraphicArea area;

//  private GPAction myTaskPropertiesAction;

  private final UIFacade myUIFacade;

  private static Pair<ResourceTreeTable, ResourceTreeTableModel> createTreeTable(IGanttProject project,
                                                                                 UIFacade uiFacade) {
    ResourceTreeTableModel model = new ResourceTreeTableModel(project.getHumanResourceManager(),
        project.getTaskManager(), project.getResourceCustomPropertyManager());
    return Pair.create(new ResourceTreeTable(project, model, uiFacade), model);
  }

  public GanttResourcePanel(final GanttProject prj, final UIFacade uiFacade, ResourceTableChartConnector resourceTableConnector) {
    super(createTreeTable(prj.getProject(), uiFacade));
    appli = prj;
    myUIFacade = uiFacade;

    prj.addProjectEventListener(getProjectEventListener());

//    getTreeTable().setupActionMaps(myResourceActionSet.getResourceMoveUpAction(),
//        myResourceActionSet.getResourceMoveDownAction(), myResourceActionSet.getResourceNewAction(), myResourceActionSet.getResourceDeleteAction(),
//        appli.getCutAction(), appli.getCopyAction(), appli.getPasteAction());
//    getTreeTable().addActionWithAccelleratorKey(myResourceActionSet.getAssignmentDelete());
//    getTreeTable().setRowHeight(20);
//    resourceTableConnector.getRowHeight().setValue(20);
//
//    getTreeTable().insertWithLeftyScrollBar(this);
    area = new ResourceLoadGraphicArea(prj, prj.getZoomManager(), this, resourceTableConnector) {
      @Override
      public boolean isExpanded(HumanResource hr) {
        return resourceTableConnector.getCollapseView().isExpanded(hr);
      }

      @Override
      protected int getRowHeight() {
        return resourceTableConnector.getMinRowHeight().intValue();
      }
    };
    resourceTableConnector.getCollapseView().getExpandedCount().addWatcher(evt -> {
      area.repaint();
      return Unit.INSTANCE;
    });
    resourceTableConnector.getTableScrollOffset().addListener(
      (ChangeListener<? super Number>) (wtf, old, newValue) -> SwingUtilities.invokeLater(() -> {
        area.getChartModel().setVerticalOffset(newValue.intValue());
        area.repaint();
        //reset();
      })
    );

    prj.getZoomManager().addZoomListener(area.getZoomListener());
    area.getChartModel().setRowHeight(resourceTableConnector.getMinRowHeight().intValue());

//    this.setBackground(new Color(0.0f, 0.0f, 0.0f));
//    updateContextActions();
    // applyComponentOrientation(lang.getComponentOrientation());
//    myRowHeightAligner = new GanttProjectBase.RowHeightAligner(this, this.area.getChartModel());
  }

  @Override
  protected void init() {
    //getTreeTable().initTreeTable();
    //area.setVScrollController(getTreeTable().getVScrollController());
  }

  @Override
  protected void handlePopupTrigger(MouseEvent e) {
  }

//  public GanttProjectBase.RowHeightAligner getRowHeightAligner() {
//    return myRowHeightAligner;
//  }

  private ProjectEventListener getProjectEventListener() {
    return new ProjectEventListener.Stub() {
      @Override
      public void projectClosed() {
        area.repaint();
        reset();
      }
    };
  }

  public ResourceTreeTable getResourceTreeTable() {
    return getTreeTable();
  }

  public ResourceTreeTableModel getResourceTreeTableModel() {
    return getTreeModel();
  }

  /**
   * Reset all human...
   */
  public void reset() {
    getTreeModel().reset();
  }

  @Override
  public void setSelected(HumanResource resource, boolean clear) {
    if (clear) {
      clearSelection();
    }
    getTree().getTreeSelectionModel().setSelectionPath(
        TreeUtil.createPath(getResourceTreeTableModel().getNodeForResource(resource)));
  }

  @Override
  public void clearSelection() {
    getTree().clearSelection();
  }

  @Override
  public AbstractAction getMoveUpAction() {
    return null;
  }

  @Override
  public AbstractAction getMoveDownAction() {
    return null;
  }

  @Override
  public TimelineChart.VScrollController getVScrollController() {
    return getTreeTable().getVScrollController();
  }

  @Override
  public void startDefaultEditing(HumanResource modelElement) {
  }

  @Override
  public AbstractAction[] getTreeActions() {
    return new AbstractAction[]{getMoveUpAction(), getMoveDownAction()};
  }

  @Override
  public void addToolbarActions(ToolbarBuilder builder) {
  }

  public GPAction getPropertiesAction() {
    return null;
  }

  @Override
  protected DefaultMutableTreeTableNode getRootNode() {
    return (DefaultMutableTreeTableNode) getTreeModel().getRoot();
  }

  @Override
  protected Chart getChart() {
    return myUIFacade.getResourceChart();
  }
}
