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

import biz.ganttproject.ganttview.ResourceTableChartConnector;
import javafx.beans.value.ChangeListener;
import kotlin.Unit;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelResource;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.ResourceChart;
import net.sourceforge.ganttproject.chart.mouse.MouseListenerBase;
import net.sourceforge.ganttproject.chart.mouse.MouseMotionListenerBase;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceEvent;
import net.sourceforge.ganttproject.resource.ResourceView;
import net.sourceforge.ganttproject.util.MouseUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Class for the graphic part of the soft
 */
public class ResourceLoadGraphicArea extends ChartComponentBase implements ResourceChart {
  /** The main application */
  final ChartModelResource myChartModel;

  private final ChartViewState myViewState;

  private final ResourceTableChartConnector tableConnector;

  public ResourceLoadGraphicArea(GanttProject app, ZoomManager zoomManager, ResourceTableChartConnector resourceTableConnector) {
    super(app.getProject(), app.getUIFacade(), zoomManager);
    this.tableConnector = resourceTableConnector;
    this.setBackground(Color.WHITE);
    myChartModel = new ChartModelResource(getTaskManager(), app.getHumanResourceManager(), getTimeUnitStack(),
        getUIConfiguration(), this);
    myChartImplementation = new ResourceChartImplementation(this, app.getProject(), getUIFacade(), myChartModel, this, resourceTableConnector);
    myViewState = new ChartViewState(this, app.getUIFacade());
    zoomManager.addZoomListener(myViewState);
    zoomManager.addZoomListener(getZoomListener());
    initMouseListeners();
    getProject().getHumanResourceManager().addView(new ResourceView() {
      @Override
      public void resourceAdded(ResourceEvent event) {

      }

      @Override
      public void resourcesRemoved(ResourceEvent event) {

      }

      @Override
      public void resourceChanged(ResourceEvent e) {
        repaint();
      }

      @Override
      public void resourceAssignmentsChanged(ResourceEvent e) {
        repaint();
      }

      @Override
      public void resourceStructureChanged() {

      }

      @Override
      public void resourceModelReset() {
        repaint();
      }
    });
    tableConnector.getCollapseView().getExpandedCount().addWatcher(evt -> {
      repaint();
      return Unit.INSTANCE;
    });
    tableConnector.getTableScrollOffset().addListener((ChangeListener<? super Number>) (wtf, old, newValue) -> SwingUtilities.invokeLater(() -> {
      getChartModel().setVerticalOffset(newValue.intValue());
      repaint();
      })
    );

    var model = getChartModel();
    var rowHeight = Math.max(
      model.calculateRowHeight(), resourceTableConnector.getMinRowHeight().getValue()
    );
    getChartModel().setRowHeight((int)rowHeight);
    app.getProject().addProjectEventListener(new ProjectEventListener.Stub() {
      @Override
      public void projectClosed() {
        repaint();
      }
    });

  }

  /** @return the preferred size of the panel. */
  @Override
  public Dimension getPreferredSize() {
    return new Dimension(465, 600);
  }

  @Override
  protected GPTreeTableBase getTreeTable() {
    return null;
  }

  @Override
  public String getName() {
    return GanttLanguage.getInstance().getText("resourcesChart");
  }

  @Override
  public void focus() {
    //myTreeUi.getTreeComponent().requestFocus();
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
    return myChartImplementation;
  }

  private MouseMotionListener myMouseMotionListener;

  private MouseListener myMouseListener;

  private final AbstractChartImplementation myChartImplementation;

  @Override
  public ChartViewState getViewState() {
    return myViewState;
  }

  @Override
  public boolean isExpanded(HumanResource hr) {
    return tableConnector.getCollapseView().isExpanded(hr);
  }

  protected int getRowHeight() {
    return tableConnector.getMinRowHeight().intValue();
  }

}
