// Copyright (C) 2021 BarD Software
package net.sourceforge.ganttproject;

import biz.ganttproject.print.PrintChartApi;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.PrintChartApiImpl;
import net.sourceforge.ganttproject.chart.export.TreeTableApiKt;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.HumanResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dbarashev@bardsoftware.com
 */
class ResourceChartImplementation extends AbstractChartImplementation {

  private final ResourceLoadGraphicArea resourceLoadGraphicArea;
  private ResourceLoadGraphicArea.ResourceChartSelection mySelection;

  public ResourceChartImplementation(
      ResourceLoadGraphicArea resourceLoadGraphicArea, IGanttProject project, UIFacade uiFacade, ChartModelBase chartModel,
      ChartComponentBase chartComponent) {
    super(project, uiFacade, chartModel, chartComponent);
    this.resourceLoadGraphicArea = resourceLoadGraphicArea;
  }

  @Override
  public void paintChart(Graphics g) {
    synchronized (ChartModelBase.STATIC_MUTEX) {
      // LaboPM
      // ResourceLoadGraphicArea.super.paintComponent(g);
      if (resourceLoadGraphicArea.isShowing()) {
        resourceLoadGraphicArea.myChartModel.setHeaderHeight(resourceLoadGraphicArea.getImplementation().getHeaderHeight());
      }
      resourceLoadGraphicArea.myChartModel.setBottomUnitWidth(resourceLoadGraphicArea.getViewState().getBottomUnitWidth());
      resourceLoadGraphicArea.myChartModel.setRowHeight(resourceLoadGraphicArea.getRowHeight());// myChartModel.setRowHeight(tree.getJTree().getRowHeight());
      resourceLoadGraphicArea.myChartModel.setTopTimeUnit(resourceLoadGraphicArea.getViewState().getTopTimeUnit());
      resourceLoadGraphicArea.myChartModel.setBottomTimeUnit(resourceLoadGraphicArea.getViewState().getBottomTimeUnit());
      // myChartModel.paint(g);
      super.paintChart(g);
    }
  }

  @Override
  public ChartSelection getSelection() {
    if (mySelection == null) {
      mySelection = new ResourceLoadGraphicArea.ResourceChartSelection(getProject(), resourceLoadGraphicArea.appli.getResourcePanel());
    }
    return mySelection;
  }

  @Override
  public IStatus canPaste(ChartSelection selection) {
    return Status.OK_STATUS;
  }

  @Override
  public void paste(ChartSelection selection) {
    if (selection instanceof ResourceLoadGraphicArea.ResourceChartSelection) {
      ResourceLoadGraphicArea.ResourceChartSelection resourceChartSelection = (ResourceLoadGraphicArea.ResourceChartSelection) selection;
      for (HumanResource res : resourceChartSelection.myClipboardContents.getResources()) {
        if (resourceChartSelection.myClipboardContents.isCut()) {
          resourceLoadGraphicArea.getResourceManager().add(res);
        } else {
          resourceLoadGraphicArea.getResourceManager().add(res.unpluggedClone());
        }
      }
    }
  }

  @Override
  public PrintChartApi asPrintChartApi() {
    ChartModelBase modelCopy = getChartModel().createCopy();
    modelCopy.setBounds(getChartComponent().getSize());
    var settingsSetup = new Function1<GanttExportSettings, Unit>() {
      @Override
      public Unit invoke(GanttExportSettings settings) {
        setupExportSettings(settings, modelCopy);
        var rowCount = new AtomicInteger(0);
        getProject().getHumanResourceManager().getResources().forEach(hr -> {
          resourceLoadGraphicArea.myTreeUi.setExpanded(hr, true);
          rowCount.addAndGet(hr.getAssignments().length + 1);
        });
        settings.setRowCount(rowCount.get());
        return Unit.INSTANCE;
      }
    };
    return new PrintChartApiImpl(modelCopy, settingsSetup,
        () -> TreeTableApiKt.asTreeTableApi(getChartComponent().getTreeTable()),
        getUIFacade().getZoomManager()
    );
  }
}
