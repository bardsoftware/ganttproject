/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Thomas Alexandre, GanttProject team

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

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelResource;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.ResourceChart;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import net.sourceforge.ganttproject.chart.gantt.ClipboardContents;
import net.sourceforge.ganttproject.chart.mouse.MouseListenerBase;
import net.sourceforge.ganttproject.chart.mouse.MouseMotionListenerBase;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.util.MouseUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Class for the graphic part of the soft
 */
public class ResourceLoadGraphicArea extends ChartComponentBase implements ResourceChart {
  /** The main application */
  private final GanttProject appli;

  private final ChartModelResource myChartModel;

  private final ChartViewState myViewState;

  private final ResourceTreeUIFacade myTreeUi;

  public ResourceLoadGraphicArea(GanttProject app, ZoomManager zoomManager, ResourceTreeUIFacade treeUi) {
    super(app.getProject(), app.getUIFacade(), zoomManager);
    appli = app;
    myTreeUi = treeUi;
    this.setBackground(Color.WHITE);
    myChartModel = new ChartModelResource(getTaskManager(), app.getHumanResourceManager(), getTimeUnitStack(),
        getUIConfiguration(), this);
    myChartImplementation = new ResourcechartImplementation(app.getProject(), getUIFacade(), myChartModel, this);
    myViewState = new ChartViewState(this, app.getUIFacade());
    app.getUIFacade().getZoomManager().addZoomListener(myViewState);
    initMouseListeners();
  }

  /** @return the preferred size of the panel. */
  @Override
  public Dimension getPreferredSize() {
    return new Dimension(465, 600);
  }

  protected int getRowHeight() {
    return appli.getResourcePanel().getRowHeight();
  }

  @Override
  protected GPTreeTableBase getTreeTable() {
    return appli.getResourcePanel().getResourceTreeTable();
  }

  @Override
  public String getName() {
    return GanttLanguage.getInstance().getText("resourcesChart");
  }

  @Override
  protected ChartModelBase getChartModel() {
    return myChartModel;
  }

  @Override
  protected MouseListener getMouseListener() {
    if (myMouseListener == null) {
      myMouseListener = new MouseListenerBase(getUIFacade(), this, getImplementation()) {
        @Override
        protected Action[] getPopupMenuActions(MouseEvent e) {
          return new Action[] { getOptionsDialogAction() };
        }

        @Override
        public void mousePressed(MouseEvent e) {
          String text = MouseUtil.toString(e);
          super.mousePressed(e);

          if (text.equals(GPAction.getKeyStrokeText("mouse.drag.chart"))) {
            startScrollView(e);
            return;
          }
        }
      };
    }
    return myMouseListener;
  }

  @Override
  protected MouseMotionListener getMouseMotionListener() {
    if (myMouseMotionListener == null) {
      myMouseMotionListener = new MouseMotionListenerBase(getUIFacade(), getImplementation());
    }
    return myMouseMotionListener;
  }

  @Override
  protected AbstractChartImplementation getImplementation() {
    if (myChartImplementation == null) {
      myChartImplementation = new ResourcechartImplementation(getProject(), getUIFacade(), myChartModel, this);
    }
    return myChartImplementation;
  }

  @Override
  public boolean isExpanded(HumanResource resource) {
    return true;
  }

  private MouseMotionListener myMouseMotionListener;

  private MouseListener myMouseListener;

  private AbstractChartImplementation myChartImplementation;

  private class ResourcechartImplementation extends AbstractChartImplementation {

    private ResourceChartSelection mySelection;

    public ResourcechartImplementation(IGanttProject project, UIFacade uiFacade, ChartModelBase chartModel,
        ChartComponentBase chartComponent) {
      super(project, uiFacade, chartModel, chartComponent);
    }

    @Override
    public void paintChart(Graphics g) {
      synchronized (ChartModelBase.STATIC_MUTEX) {
        // LaboPM
        // ResourceLoadGraphicArea.super.paintComponent(g);
        if (isShowing()) {
          myChartModel.setHeaderHeight(getImplementation().getHeaderHeight(appli.getResourcePanel(),
              appli.getResourcePanel().getTreeTable().getScrollPane().getViewport()));
        }
        myChartModel.setBottomUnitWidth(getViewState().getBottomUnitWidth());
        myChartModel.setRowHeight(getRowHeight());// myChartModel.setRowHeight(tree.getJTree().getRowHeight());
        myChartModel.setTopTimeUnit(getViewState().getTopTimeUnit());
        myChartModel.setBottomTimeUnit(getViewState().getBottomTimeUnit());
        // myChartModel.paint(g);
        super.paintChart(g);
      }
    }

    @Override
    public ChartSelection getSelection() {
      if (mySelection == null) {
        mySelection = new ResourceChartSelection(getProject(), appli.getResourcePanel());
      }
      return mySelection;
    }

    @Override
    public IStatus canPaste(ChartSelection selection) {
      return Status.OK_STATUS;
    }

    @Override
    public void paste(ChartSelection selection) {
      if (selection instanceof ResourceChartSelection) {
        ResourceChartSelection resourceChartSelection = (ResourceChartSelection) selection;
        for (HumanResource res : resourceChartSelection.myClipboardContents.getResources()) {
          if (resourceChartSelection.myClipboardContents.isCut()) {
            getResourceManager().add(res);
          } else {
            getResourceManager().add(res.unpluggedClone());
          }
        }
      }
    }

    @Override
    public void buildImage(GanttExportSettings settings, ChartImageVisitor imageVisitor) {
      int rowCount = getResourceManager().getResources().size();
      for (HumanResource hr : getResourceManager().getResources()) {
        if (settings.isExpanded(hr)) {
          myTreeUi.setExpanded(hr, true);
        }
        if (myTreeUi.isExpanded(hr)) {
          rowCount += hr.getAssignments().length;
        }
      }
      settings.setRowCount(rowCount);
      super.buildImage(settings, imageVisitor);
    }
  }

  @Override
  public ChartViewState getViewState() {
    return myViewState;
  }

  private HumanResourceManager getResourceManager() {
    return appli.getHumanResourceManager();
  }

  static class ResourceChartSelection extends AbstractChartImplementation.ChartSelectionImpl implements ClipboardOwner {
    private final GanttResourcePanel myResourcePanel;
    private final IGanttProject myProject;
    private ClipboardContents myClipboardContents;

    ResourceChartSelection(IGanttProject project, GanttResourcePanel resourcePanel) {
      myProject = project;
      myResourcePanel = resourcePanel;
    }
    @Override
    public boolean isEmpty() {
      return myResourcePanel.getSelectedNodes().length == 0;
    }

    @Override
    public void startCopyClipboardTransaction() {
      super.startCopyClipboardTransaction();
      myClipboardContents = new ClipboardContents(myProject.getTaskManager());
      myResourcePanel.copySelection(myClipboardContents);
      exportIntoSystemClipboard();
    }

    @Override
    public void startMoveClipboardTransaction() {
      super.startMoveClipboardTransaction();
      myClipboardContents = new ClipboardContents(myProject.getTaskManager());
      myResourcePanel.cutSelection(myClipboardContents);
      exportIntoSystemClipboard();
    }

    private void exportIntoSystemClipboard() {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(new GPTransferable(myClipboardContents), this);
    }

    @Override
    public void cancelClipboardTransaction() {
      super.cancelClipboardTransaction();
      myClipboardContents = null;
    }

    @Override
    public void commitClipboardTransaction() {
      super.commitClipboardTransaction();
      myClipboardContents = null;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable transferable) {
      // Do nothing.
    }
  }

}