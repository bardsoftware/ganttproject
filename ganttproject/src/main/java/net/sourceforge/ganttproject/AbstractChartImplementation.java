/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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

import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.option.FontOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.IntegerOption;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartRendererBase;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.chart.ChartUIConfiguration;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.export.ChartDimensions;
import net.sourceforge.ganttproject.chart.export.ChartImageBuilder;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import net.sourceforge.ganttproject.chart.export.RenderedChartImage;
import net.sourceforge.ganttproject.chart.mouse.MouseInteraction;
import net.sourceforge.ganttproject.chart.mouse.ScrollViewInteraction;
import net.sourceforge.ganttproject.chart.mouse.TimelineFacadeImpl;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.zoom.ZoomEvent;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class AbstractChartImplementation implements TimelineChart, ZoomListener {
  private final ChartModelBase myChartModel;
  private final IGanttProject myProject;
  private Set<ChartSelectionListener> mySelectionListeners = new LinkedHashSet<>();
  private final ChartComponentBase myChartComponent;
  private MouseInteraction myActiveInteraction;
  private final UIFacade myUiFacade;
  private VScrollController myVScrollController;
  private final Timer myTimer = new Timer();
  private Runnable myTimerTask = null;

  public class MouseHoverLayerUi extends LayerUI<ChartComponentBase> {
    private Point myHoverPoint;

    @Override
    public void installUI(JComponent c) {
      super.installUI(c);
      JLayer jlayer = (JLayer)c;
      jlayer.setLayerEventMask(
        AWTEvent.MOUSE_MOTION_EVENT_MASK
      );
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e, JLayer l) {
      myHoverPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), l);
      l.repaint();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
      Graphics2D g2 = (Graphics2D)g.create();
      super.paint(g2, c);
      if (myHoverPoint == null) {
        return;
      }
      ChartModelBase chartModel = getChartModel();
      if (chartModel.getBottomUnit() == GPTimeUnitStack.DAY) {
        return;
      }
      Offset offset = chartModel.getOffsetAt(myHoverPoint.x);
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .4f));
      Font chartFont = chartModel.getChartUIConfiguration().getChartFont();
      g2.setFont(chartFont.deriveFont(0.9f * chartFont.getSize()));
      g2.setColor(Color.BLACK);
      int offsetMidPx = (offset.getStartPixels() + offset.getOffsetPixels()) / 2;
      int headerBottomPx = chartModel.getChartUIConfiguration().getHeaderHeight();
      int pointerSize = (int)(chartModel.getChartUIConfiguration().getBaseFontSize() * 0.6f);
      int[] xPoints = new int[] {offsetMidPx - pointerSize/2, offsetMidPx, offsetMidPx + pointerSize/2};
      int[] yPoints = new int[] {headerBottomPx + pointerSize, headerBottomPx, headerBottomPx + pointerSize};

      g2.fillPolygon(xPoints, yPoints, 3);
      g2.drawString(GanttLanguage.getInstance().formatShortDate(CalendarFactory.createGanttCalendar(offset.getOffsetStart())),
          offsetMidPx, headerBottomPx + (int)(chartModel.getChartUIConfiguration().getBaseFontSize() * 1.4f));
    }
  }

  public AbstractChartImplementation(IGanttProject project, UIFacade uiFacade, ChartModelBase chartModel,
      ChartComponentBase chartComponent) {
    assert chartModel != null;
    myUiFacade = uiFacade;
    myChartModel = chartModel;
    myProject = project;

    myChartComponent = chartComponent;
    uiFacade.getTaskSelectionManager().addSelectionListener(new TaskSelectionManager.Listener() {
      @Override
      public void userInputConsumerChanged(Object newConsumer) {
        fireSelectionChanged();
      }

      @Override
      public void selectionChanged(List<Task> currentSelection) {
        fireSelectionChanged();
      }
    });
    myTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        if (myTimerTask != null) {
          SwingUtilities.invokeLater(myTimerTask);
          myTimerTask = null;
        }
      }
    }, 1000, 1000);
  }


  @Override
  public void init(IGanttProject project, IntegerOption dpiOption, FontOption chartFontOption) {
    // Skip as we already have a project instance.
  }

  protected void setCursor(Cursor cursor) {
    myChartComponent.setCursor(cursor);
  }

  protected UIFacade getUIFacade() {
    return myUiFacade;
  }

  @Override
  public IGanttProject getProject() {
    return myProject;
  }

  @Override
  public void setVScrollController(TimelineChart.VScrollController vscrollController) {
    myVScrollController = vscrollController;
  }

  public void beginScrollViewInteraction(MouseEvent e) {
    TimelineFacadeImpl timelineFacade = new TimelineFacadeImpl(getChartModel(), myProject.getTaskManager());
    timelineFacade.setVScrollController(myVScrollController);
    setActiveInteraction(new ScrollViewInteraction(e, timelineFacade));
  }

  public MouseInteraction finishInteraction() {
    try {
      if (getActiveInteraction() != null) {
        getActiveInteraction().finish();
      }
      return getActiveInteraction();
    } finally {
      setActiveInteraction(null);
    }
  }

  protected void setActiveInteraction(MouseInteraction activeInteraction) {
    if (myActiveInteraction != null) {
      myActiveInteraction.finish();
    }
    myActiveInteraction = activeInteraction;
  }

  public MouseInteraction getActiveInteraction() {
    return myActiveInteraction;
  }

  @Override
  public void zoomChanged(ZoomEvent e) {
    myChartComponent.invalidate();
    myChartComponent.repaint();
  }

  public void paintChart(Graphics g) {
    getChartModel().paint(g);
  }

  protected ChartModelBase getChartModel() {
    return myChartModel;
  }

  protected void scheduleTask(Runnable task) {
    myTimerTask = task;
  }

  protected ChartComponentBase getChartComponent() {
    return myChartComponent;
  }

  private Image getLogo() {
    return myUiFacade.getLogo();
  }
  // ///////////////////////////////////////////////////////////
  // interface Chart
  @Override
  public void buildImage(GanttExportSettings settings, ChartImageVisitor imageVisitor) {
    ChartModelBase modelCopy = getChartModel().createCopy();
    modelCopy.setBounds(myChartComponent.getSize());
    if (settings.getStartDate() == null) {
      settings.setStartDate(modelCopy.getStartDate());
    }
    if (settings.getEndDate() == null) {
      settings.setEndDate(modelCopy.getEndDate());
    }
    if (settings.isCommandLineMode()) {
      myChartComponent.getTreeTable().getTable().getTableHeader().setVisible(true);
      myChartComponent.getTreeTable().doLayout();
      myChartComponent.getTreeTable().getTable().setRowHeight(modelCopy.calculateRowHeight());
      myChartComponent.getTreeTable().autoFitColumns();
    }
    settings.setLogo(getLogo());
    ChartImageBuilder builder = new ChartImageBuilder(settings, modelCopy, myChartComponent.getTreeTable());
    builder.buildImage(imageVisitor);
  }

  @Override
  public RenderedImage getRenderedImage(GanttExportSettings settings) {
    class ChartImageVisitorImpl implements ChartImageVisitor {
      private RenderedChartImage myRenderedImage;
      private Graphics2D myGraphics;
      private BufferedImage myTreeImage;

      @Override
      public void acceptLogo(ChartDimensions d, Image logo) {
        if (d.getTreeWidth() <= 0) {
          return;
        }
        Graphics2D g = getGraphics(d);
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, d.getTreeWidth(), d.getLogoHeight());
        // Hack: by adding 35, the left part of the logo becomes visible,
        // otherwise it gets chopped off
        g.drawImage(logo, 35, 0, null);
      }

      @Override
      public void acceptTable(ChartDimensions d, Component header, Component table) {
        if (d.getTreeWidth() <= 0) {
          return;
        }
        Graphics2D g = getGraphics(d);
        g.translate(0, d.getLogoHeight());
        header.print(g);

        g.translate(0, d.getTableHeaderHeight());
        table.print(g);
      }

      @Override
      public void acceptChart(ChartDimensions d, ChartModel model) {
        if (myTreeImage == null) {
          myTreeImage = new BufferedImage(1, d.getChartHeight() + d.getLogoHeight(), BufferedImage.TYPE_INT_RGB);
        }
        myRenderedImage = new RenderedChartImage(model, myTreeImage, d.getChartWidth(), d.getChartHeight()
            + d.getLogoHeight(), d.getLogoHeight());
      }

      private Graphics2D getGraphics(ChartDimensions d) {
        if (myGraphics == null) {
          myTreeImage = new BufferedImage(d.getTreeWidth(), d.getChartHeight() + d.getLogoHeight(),
              BufferedImage.TYPE_INT_RGB);
          myGraphics = myTreeImage.createGraphics();
        }
        return myGraphics;
      }
    }
    ChartImageVisitorImpl visitor = new ChartImageVisitorImpl();
    buildImage(settings, visitor);
    return visitor.myRenderedImage;
  }

  @Override
  public Date getStartDate() {
    return getChartModel().getStartDate();
  }

  @Override
  public void setStartDate(Date startDate) {
    startDate = getBottomTimeUnit().adjustLeft(startDate);
    getChartModel().setStartDate(startDate);
  }

  @Override
  public void scrollBy(TimeDuration duration) {
    setStartDate(getChartModel().getTaskManager().shift(getStartDate(), duration));
  }

  @Override
  public void setStartOffset(int pixels) {
    getChartModel().setHorizontalOffset(pixels);
  }

  private TimeUnit getBottomTimeUnit() {
    return getChartModel().getBottomUnit();
  }

  @Override
  public Date getEndDate() {
    return getChartModel().getEndDate();
  }

  @Override
  public void setDimensions(int height, int width) {
    Dimension bounds = new Dimension(width, height);
    getChartModel().setBounds(bounds);
  }

  @Override
  public void setBottomUnit(TimeUnit bottomUnit) {
    getChartModel().setBottomTimeUnit(bottomUnit);
  }

  @Override
  public void setTopUnit(TimeUnit topUnit) {
    getChartModel().setTopTimeUnit(topUnit);
  }

  @Override
  public void setBottomUnitWidth(int width) {
    getChartModel().setBottomUnitWidth(width);
  }

  @Override
  public String getName() {
    return myChartComponent.getName();
  }

  @Override
  public void reset() {
    myChartComponent.reset();
  }

  @Override
  public GPOptionGroup[] getOptionGroups() {
    return getChartModel().getChartOptionGroups();
  }

  @Override
  public Chart createCopy() {
    return new AbstractChartImplementation(myProject, myUiFacade, getChartModel().createCopy(), myChartComponent);
  }

  @Override
  public Object getAdapter(Class arg0) {
    return null;
  }

  @Override
  public ChartSelection getSelection() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IStatus canPaste(ChartSelection selection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void paste(ChartSelection selection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addSelectionListener(ChartSelectionListener listener) {
    mySelectionListeners.add(listener);
  }

  @Override
  public void removeSelectionListener(ChartSelectionListener listener) {
    mySelectionListeners.remove(listener);
  }

  private void fireSelectionChanged() {
    for (ChartSelectionListener nextListener : mySelectionListeners) {
      nextListener.selectionChanged();
    }
  }

  @Override
  public void addRenderer(ChartRendererBase renderer) {
    myChartModel.addRenderer(renderer);
  }

  @Override
  public void resetRenderers() {
    myChartModel.resetRenderers();
  }

  @Override
  public ChartModel getModel() {
    return myChartModel;
  }

  @Override
  public ChartUIConfiguration getStyle() {
    return myChartModel.getChartUIConfiguration();
  }

  private Integer myCachedHeaderHeight = 30;
  int getHeaderHeight(final JComponent tableContainer, final JComponent table) {
    return myCachedHeaderHeight;
  }
  public void setTimelineHeight(int height) {
    myCachedHeaderHeight = height;
  }


  public abstract static class ChartSelectionImpl implements ChartSelection {
    private boolean isTransactionRunning;

    @Override
    public abstract boolean isEmpty();

    @Override
    public IStatus isDeletable() {
      return Status.OK_STATUS;
    }

    @Override
    public void startCopyClipboardTransaction() {
      if (isTransactionRunning) {
        cancelClipboardTransaction();
      }
      isTransactionRunning = true;
    }

    @Override
    public void startMoveClipboardTransaction() {
      if (isTransactionRunning) {
        cancelClipboardTransaction();
      }
      isTransactionRunning = true;
    }

    @Override
    public void cancelClipboardTransaction() {
      isTransactionRunning = false;
    }

    @Override
    public void commitClipboardTransaction() {
      isTransactionRunning = false;
    }

  }

  MouseHoverLayerUi createMouseHoverLayer() {
    return new MouseHoverLayerUi();
  }
}
