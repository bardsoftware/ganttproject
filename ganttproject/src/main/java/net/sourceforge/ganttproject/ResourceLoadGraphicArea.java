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
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelResource;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.ResourceChart;
import net.sourceforge.ganttproject.chart.mouse.MouseListenerBase;
import net.sourceforge.ganttproject.chart.mouse.MouseMotionListenerBase;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
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
  final GanttProject appli;

  final ChartModelResource myChartModel;

  private final ChartViewState myViewState;

  final ResourceTreeUIFacade myTreeUi;

  public ResourceLoadGraphicArea(GanttProject app, ZoomManager zoomManager, ResourceTreeUIFacade treeUi, ResourceTableChartConnector resourceTableConnector) {
    super(app.getProject(), app.getUIFacade(), zoomManager);
    appli = app;
    myTreeUi = treeUi;
    this.setBackground(Color.WHITE);
    myChartModel = new ChartModelResource(getTaskManager(), app.getHumanResourceManager(), getTimeUnitStack(),
        getUIConfiguration(), this);
    myChartImplementation = new ResourceChartImplementation(this, app.getProject(), getUIFacade(), myChartModel, this, resourceTableConnector);
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
  public void focus() {
    myTreeUi.getTreeComponent().requestFocus();
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
    return myChartImplementation;
  }

  @Override
  public boolean isExpanded(HumanResource resource) {
    return true;
  }

  private MouseMotionListener myMouseMotionListener;

  private MouseListener myMouseListener;

  private final AbstractChartImplementation myChartImplementation;

  @Override
  public ChartViewState getViewState() {
    return myViewState;
  }

  HumanResourceManager getResourceManager() {
    return appli.getHumanResourceManager();
  }

}
