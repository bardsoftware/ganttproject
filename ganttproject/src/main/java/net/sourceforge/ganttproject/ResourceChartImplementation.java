// Copyright (C) 2021 BarD Software
package net.sourceforge.ganttproject;

import biz.ganttproject.ganttview.ResourceTableChartConnector;
import biz.ganttproject.print.PrintChartApi;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.PrintChartApiImpl;
import net.sourceforge.ganttproject.gui.UIFacade;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dbarashev@bardsoftware.com
 */
class ResourceChartImplementation extends AbstractChartImplementation {

  private final ResourceLoadGraphicArea resourceLoadGraphicArea;
  private final ResourceTableChartConnector resourceTableConnector;

  public ResourceChartImplementation(
    ResourceLoadGraphicArea resourceLoadGraphicArea, IGanttProject project, UIFacade uiFacade, ChartModelBase chartModel,
    ChartComponentBase chartComponent, ResourceTableChartConnector resourceTableConnector) {
    super(project, uiFacade, chartModel, chartComponent);
    this.resourceLoadGraphicArea = resourceLoadGraphicArea;
    this.resourceTableConnector = resourceTableConnector;
    setVScrollController(new VScrollController() {
      @Override
      public boolean isScrollable() {
        return true;
      }

      @Override
      public void scrollBy(int pixels) {
        var scrollConsumer = resourceTableConnector.getChartScrollOffset();
        if (scrollConsumer != null) {
          scrollConsumer.accept(0.0 + pixels);
        }
      }
    });

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
  public PrintChartApi asPrintChartApi() {
    ChartModelBase modelCopy = getChartModel().createCopy();
    modelCopy.setBounds(getChartComponent().getSize());
    var rowHeight = Math.max(
      modelCopy.calculateRowHeight(), resourceTableConnector.getMinRowHeight().getValue()
    );
    modelCopy.setRowHeight((int)rowHeight);
    resourceTableConnector.getRowHeight().setValue(rowHeight);

    var settingsSetup = new Function1<GanttExportSettings, Unit>() {
      @Override
      public Unit invoke(GanttExportSettings settings) {
        setupExportSettings(settings, modelCopy);
        var rowCount = new AtomicInteger(0);
        getProject().getHumanResourceManager().getResources().forEach(hr -> {
          //resourceLoadGraphicArea.myTreeUi.setExpanded(hr, true);
          rowCount.addAndGet(hr.getAssignments().length + 1);
        });
        settings.setRowCount(rowCount.get());
        return Unit.INSTANCE;
      }
    };
    return new PrintChartApiImpl(modelCopy, settingsSetup,
        resourceTableConnector.getExportTreeTableApi(),
        getUIFacade().getZoomManager()
    );
  }
}
