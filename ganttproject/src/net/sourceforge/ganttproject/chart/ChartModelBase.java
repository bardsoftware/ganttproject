/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.chart;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.chart.OffsetManager.OffsetBuilderFactory;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TimelineLabelChartItem;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionChangeListener;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeDuration;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitFunctionOfDate;
import net.sourceforge.ganttproject.time.TimeUnitStack;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Controls painting of the common part of Gantt and resource charts (in
 * particular, timeline). Calculates data required by the specific charts (e.g.
 * calculates the offsets of the timeline grid cells)
 */
public abstract class ChartModelBase implements /* TimeUnitStack.Listener, */ChartModel, TimelineLabelRendererImpl.ChartModelApi {
  public static interface ScrollingSession {
    void scrollTo(int xpos, int ypos);

    void finish();
  }

  private class ScrollingSessionImpl implements ScrollingSession {
    private int myPrevXpos;

    private OffsetList myTopOffsets;
    private OffsetList myBottomOffsets;
    private OffsetList myDefaultOffsets;

    private ScrollingSessionImpl(int startXpos) {
      // System.err.println("start xpos=" + startXpos);
      myPrevXpos = startXpos;
      ChartModelBase.this.myScrollingSession = this;
      ChartModelBase.this.myOffsetManager.reset();
      myTopOffsets = (OffsetList) getTopUnitOffsets();
      myBottomOffsets = getBottomUnitOffsets();
      myDefaultOffsets = (OffsetList) getDefaultUnitOffsets();
      // shiftOffsets(-myBottomOffsets.get(0).getOffsetPixels());
      // System.err.println(myBottomOffsets.subList(0, 3));
    }

    @Override
    public void scrollTo(int xpos, int ypos) {
      int shift = xpos - myPrevXpos;
      // System.err.println("xpos="+xpos+" shift=" + shift);
      shiftOffsets(shift);
      if (myBottomOffsets.get(0).getOffsetPixels() > 0) {
        int currentExceed = myBottomOffsets.get(0).getOffsetPixels();
        ChartModelBase.this.setStartDate(getBottomUnit().jumpLeft(getStartDate()));
        ChartModelBase.this.myOffsetManager.constructOffsets();
        shiftOffsets(-myBottomOffsets.get(1).getOffsetPixels() + currentExceed);
        // System.err.println("one time unit to the left. start date=" +
        // ChartModelBase.this.getStartDate());
        // System.err.println(myBottomOffsets.subList(0, 3));
      } else if (myBottomOffsets.get(1).getOffsetPixels() <= 0) {
        ChartModelBase.this.setStartDate(myBottomOffsets.get(2).getOffsetStart());
        ChartModelBase.this.myOffsetManager.constructOffsets();
        shiftOffsets(-myBottomOffsets.get(0).getOffsetPixels());
        // System.err.println("one time unit to the right. start date=" +
        // ChartModelBase.this.getStartDate());
        // System.err.println(myBottomOffsets.subList(0, 3));
      }
      myPrevXpos = xpos;
    }

    @Override
    public void finish() {
      Offset offset0 = myBottomOffsets.get(0);
      Offset offset1 = myBottomOffsets.get(1);
      int middle = (offset1.getOffsetPixels() + offset0.getOffsetPixels()) / 2;
      if (middle < 0) {
        ChartModelBase.this.setStartDate(myBottomOffsets.get(2).getOffsetStart());
      }

      ChartModelBase.this.myScrollingSession = null;
    }

    private void shiftOffsets(int shiftPixels) {
      myBottomOffsets.shift(shiftPixels);
      myTopOffsets.shift(shiftPixels);
      if (myDefaultOffsets != myBottomOffsets) {
        if (myDefaultOffsets.isEmpty()) {
          myDefaultOffsets = (OffsetList) ChartModelBase.this.getDefaultUnitOffsets();
        }
        myDefaultOffsets.shift(shiftPixels);
      }
    }
  }

  class OffsetBuilderImpl extends RegularFrameOffsetBuilder {
    private final boolean isCompressedWeekend;

    public OffsetBuilderImpl(ChartModelBase model, int width, Date endDate) {
      super(
          model.getTaskManager().getCalendar(),
          model.getBottomUnit(),
          model.getTimeUnitStack().getDefaultTimeUnit(),
          model.getOffsetAnchorDate(),
          model.getStartDate(),
          model.getBottomUnitWidth(),
          width,
          model.getTopUnit().isConstructedFrom(model.getBottomUnit()) ? RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR
              : 1f, endDate, 0);
      isCompressedWeekend = model.getTopUnit().isConstructedFrom(model.getBottomUnit());
    }

    @Override
    protected void calculateNextStep(OffsetStep step, TimeUnit timeUnit, Date startDate) {
      float offsetStep = getOffsetStep(timeUnit);
      DayType dayType = ChartModelBase.this.getTaskManager().getCalendar().getDayTypeDate(startDate);
      step.dayType = dayType;
      if (dayType != DayType.WORKING && isCompressedWeekend) {
        offsetStep = offsetStep / RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR;
      }
      step.parrots += offsetStep;
    }

    @Override
    protected float getOffsetStep(TimeUnit timeUnit) {
      int offsetUnitCount = timeUnit.getAtomCount(getTimeUnitStack().getDefaultTimeUnit());
      return 1f / offsetUnitCount;
    }
  }

  public static final Object STATIC_MUTEX = new Object();

  private static final Predicate<? super Task> MILESTONE_PREDICATE = new Predicate<Task>() {
    @Override
    public boolean apply(Task input) {
      return input.isMilestone();
    }
  };

  private final OptionEventDispatcher myOptionEventDispatcher = new OptionEventDispatcher();

  private Dimension myBounds;

  private Date myStartDate;

  protected int myAtomUnitPixels;

  protected final TimeUnitStack myTimeUnitStack;

  private TimeUnit myTopUnit;

  protected TimeUnit myBottomUnit;

  private final ChartHeaderImpl myChartHeader;
  private final BackgroundRendererImpl myBackgroundRenderer;

  private final StyledPainterImpl myPainter;

  private final List<GPOptionChangeListener> myOptionListeners = new ArrayList<GPOptionChangeListener>();

  private final UIConfiguration myProjectConfig;
  private ChartUIConfiguration myChartUIConfiguration;

  private final List<ChartRendererBase> myRenderers = new ArrayList<ChartRendererBase>();

  private final ChartDayGridRenderer myChartGrid;

  private final TimelineLabelRendererImpl myTimelineLabelRenderer;

  protected final TaskManager myTaskManager;

  private int myVerticalOffset;

  private int myHorizontalOffset;

  private ScrollingSessionImpl myScrollingSession;

  private Set<Task> myTimelineTasks = Collections.emptySet();

  private final GPOptionGroup myTimelineLabelOptions;

  private final BooleanOption myTimelineMilestonesOption = new DefaultBooleanOption("timeline.showMilestones", true);

  public ChartModelBase(TaskManager taskManager, TimeUnitStack timeUnitStack, UIConfiguration projectConfig) {
    myTaskManager = taskManager;
    myProjectConfig = projectConfig;
    myChartUIConfiguration = new ChartUIConfiguration(projectConfig);
    myPainter = new StyledPainterImpl(myChartUIConfiguration);
    myTimeUnitStack = timeUnitStack;
    myChartHeader = new ChartHeaderImpl(this);
    myChartGrid = new ChartDayGridRenderer(this, projectConfig, myChartHeader.getTimelineContainer());
    myBackgroundRenderer = new BackgroundRendererImpl(this);
    myTimelineLabelOptions = new ChartOptionGroup("timelineLabels", new GPOption[] { myTimelineMilestonesOption }, getOptionEventDispatcher());
    myTimelineLabelRenderer = new TimelineLabelRendererImpl(this);
    addRenderer(myBackgroundRenderer);
    addRenderer(myChartHeader);
    addRenderer(myChartGrid);
    addRenderer(myTimelineLabelRenderer);
  }

  private OffsetManager myOffsetManager = new OffsetManager(new OffsetBuilderFactory() {
    @Override
    public OffsetBuilder createTopAndBottomUnitBuilder() {
      return createOffsetBuilderFactory().build();
    }

    @Override
    public OffsetBuilderImpl createAtomUnitBuilder() {
      OffsetBuilderImpl offsetBuilder = new OffsetBuilderImpl(ChartModelBase.this, getBounds() == null ? 0
          : (int) getBounds().getWidth(), null);
      int defaultUnitCountPerLastBottomUnit = RegularFrameOffsetBuilder.getConcreteUnit(getBottomUnit(), getEndDate()).getAtomCount(
          getDefaultUnit());
      offsetBuilder.setRightMarginBottomUnitCount(myScrollingSession == null ? 0
          : defaultUnitCountPerLastBottomUnit * 2);
      return offsetBuilder;
    }
  });

  @Override
  public List<Offset> getTopUnitOffsets() {
    return myOffsetManager.getTopUnitOffsets();
  }

  @Override
  public OffsetList getBottomUnitOffsets() {
    return myOffsetManager.getBottomUnitOffsets();
  }

  @Override
  public List<Offset> getDefaultUnitOffsets() {
    if (getBottomUnit().equals(getTimeUnitStack().getDefaultTimeUnit())) {
      return getBottomUnitOffsets();
    }
    return myOffsetManager.getAtomUnitOffsets();
  }

  Date getOffsetAnchorDate() {
    return /*
            * myScrollingSession == null ? myStartDate :
            */getBottomUnit().jumpLeft(myStartDate);
  }

  public OffsetBuilder.Factory createOffsetBuilderFactory() {
    OffsetBuilder.Factory factory = new RegularFrameOffsetBuilder.FactoryImpl().withAtomicUnitWidth(
        getBottomUnitWidth()).withBottomUnit(getBottomUnit()).withCalendar(myTaskManager.getCalendar()).withRightMargin(
        myScrollingSession == null ? 0 : 1).withStartDate(getOffsetAnchorDate()).withViewportStartDate(getStartDate()).withTopUnit(
        myTopUnit).withWeekendDecreaseFactor(
        getTopUnit().isConstructedFrom(getBottomUnit()) ? RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR
            : 1f);
    if (getBounds() != null) {
      factory.withEndOffset((int) getBounds().getWidth());
    }
    return factory;
  }

  @Override
  public void paint(Graphics g) {
    int height = (int) getBounds().getHeight();
    for (ChartRendererBase renderer : getRenderers()) {
      renderer.clear();
      renderer.setHeight(height);
    }
    for (ChartRendererBase renderer : getRenderers()) {
      renderer.render();
    }
    myPainter.setGraphics(g);
    for (ChartRendererBase renderer : getRenderers()) {
      renderer.getPrimitiveContainer().paint(myPainter);
    }
    for (int layer = 0;; layer++) {
      boolean layerPainted = false;
      for (ChartRendererBase renderer : getRenderers()) {
        List<GraphicPrimitiveContainer> layers = renderer.getPrimitiveContainer().getLayers();
        if (layer < layers.size()) {
          layers.get(layer).paint(myPainter);
          layerPainted = true;
        }
      }
      if (!layerPainted) {
        break;
      }
    }
  }

  protected List<ChartRendererBase> getRenderers() {
    return myRenderers;
  }

  @Override
  public void addRenderer(ChartRendererBase renderer) {
    myRenderers.add(renderer);
  }

  protected Painter getPainter() {
    return myPainter;
  }

  public void resetRenderers() {
    myRenderers.clear();
  }

  @Override
  public void setBounds(Dimension bounds) {
    if (bounds != null && bounds.equals(myBounds)) {
      return;
    }
    myBounds = bounds;
    myOffsetManager.reset();
  }

  @Override
  public void setStartDate(Date startDate) {
    myHorizontalOffset = 0;
    if (!startDate.equals(myStartDate)) {
      myStartDate = startDate;
      myOffsetManager.reset();
    }
  }

  @Override
  public Date getStartDate() {
    return myStartDate;
  }

  @Override
  public Date getEndDate() {
    List<Offset> offsets = getBottomUnitOffsets();
    return offsets.isEmpty() ? null : offsets.get(offsets.size() - 1).getOffsetEnd();
  }

  @Override
  public void setBottomUnitWidth(int pixelsWidth) {
    if (pixelsWidth == myAtomUnitPixels) {
      return;
    }
    myAtomUnitPixels = pixelsWidth;
    myOffsetManager.reset();
  }

  @Override
  public void setRowHeight(int rowHeight) {
    getChartUIConfiguration().setRowHeight(rowHeight);
  }

  @Override
  public void setTopTimeUnit(TimeUnit topTimeUnit) {
    setTopUnit(topTimeUnit);
  }

  @Override
  public void setBottomTimeUnit(TimeUnit bottomTimeUnit) {
    if (bottomTimeUnit.equals(myBottomUnit)) {
      return;
    }
    myBottomUnit = bottomTimeUnit;
    myOffsetManager.reset();
  }

  protected UIConfiguration getProjectConfig() {
    return myProjectConfig;
  }

  @Override
  public Dimension getBounds() {
    return myBounds;
  }

  // @Override
  // public Dimension getMaxBounds() {
  // OffsetBuilderImpl offsetBuilder = new OffsetBuilderImpl(
  // this, Integer.MAX_VALUE, getTaskManager().getProjectEnd());
  // List<Offset> topUnitOffsets = new ArrayList<Offset>();
  // OffsetList bottomUnitOffsets = new OffsetList();
  // offsetBuilder.constructOffsets(topUnitOffsets, bottomUnitOffsets);
  // int width = topUnitOffsets.get(topUnitOffsets.size()-1).getOffsetPixels();
  // int height = calculateRowHeight()*getRowCount();
  // return new Dimension(width, height);
  // }

  public abstract int calculateRowHeight();

  // protected abstract int getRowCount();

  @Override
  public int getBottomUnitWidth() {
    return myAtomUnitPixels;
  }

  @Override
  public TimeUnitStack getTimeUnitStack() {
    return myTimeUnitStack;
  }

  @Override
  public ChartUIConfiguration getChartUIConfiguration() {
    return myChartUIConfiguration;
  }

  @Override
  public int getTimelineTopLineHeight() {
    return getChartUIConfiguration().getSpanningHeaderHeight();
  }

  private void setChartUIConfiguration(ChartUIConfiguration chartConfig) {
    myChartUIConfiguration = chartConfig;
  }

  @Override
  public TaskManager getTaskManager() {
    return myTaskManager;
  }

  @Override
  public ChartHeader getChartHeader() {
    return myChartHeader;
  }

  @Override
  public Offset getOffsetAt(int x) {
    for (Offset offset : getDefaultUnitOffsets()) {
      if (offset.getOffsetPixels() >= x) {
        // System.err.println("result=" + offset);
        return offset;
      }
    }
    List<Offset> offsets = getBottomUnitOffsets();
    return offsets.get(offsets.size() - 1);
  }

  /**
   * @return A length of the visible part of this chart area measured in the
   *         bottom line time units
   */
  public TimeDuration getVisibleLength() {
    double pixelsLength = getBounds().getWidth();
    float unitsLength = (float) (pixelsLength / getBottomUnitWidth());
    TimeDuration result = getTaskManager().createLength(getBottomUnit(), unitsLength);
    return result;
  }

  public void setHeaderHeight(int i) {
    getChartUIConfiguration().setHeaderHeight(i);
  }

  @Override
  public void setVerticalOffset(int offset) {
    myVerticalOffset = offset;
  }

  protected int getVerticalOffset() {
    return myVerticalOffset;
  }

  public void setHorizontalOffset(int pixels) {
    myHorizontalOffset = pixels;
  }

  protected int getHorizontalOffset() {
    return myHorizontalOffset;
  }

  @Override
  public TimeUnit getBottomUnit() {
    return myBottomUnit;
  }

  private TimeUnit getDefaultUnit() {
    return getTimeUnitStack().getDefaultTimeUnit();
  }

  private void setTopUnit(TimeUnit topUnit) {
    if (topUnit.equals(myTopUnit)) {
      return;
    }
    this.myTopUnit = topUnit;
    myOffsetManager.reset();
  }

  public TimeUnit getTopUnit() {
    return getTopUnit(myStartDate);
  }

  private TimeUnit getTopUnit(Date startDate) {
    TimeUnit result = myTopUnit;
    if (myTopUnit instanceof TimeUnitFunctionOfDate) {
      if (startDate == null) {
        throw new RuntimeException("No date is set");
      }
      result = ((TimeUnitFunctionOfDate) myTopUnit).createTimeUnit(startDate);
    }
    return result;
  }

  public GPOptionGroup[] getChartOptionGroups() {
    return new GPOptionGroup[] { myChartGrid.getOptions(), myTimelineLabelOptions };
  }

  public void addOptionChangeListener(GPOptionChangeListener listener) {
    myOptionListeners.add(listener);
  }

  protected void fireOptionsChanged() {
    for (GPOptionChangeListener next : myOptionListeners) {
      next.optionsChanged();
    }
  }

  public abstract ChartModelBase createCopy();

  protected void setupCopy(ChartModelBase copy) {
    copy.setTopTimeUnit(getTopUnit());
    copy.setBottomTimeUnit(getBottomUnit());
    copy.setBottomUnitWidth(getBottomUnitWidth());
    copy.setStartDate(getStartDate());
    copy.setChartUIConfiguration(myChartUIConfiguration.createCopy());
    copy.setBounds(getBounds());
    GPOptionGroup[] copyOptions = copy.getChartOptionGroups();
    GPOptionGroup[] thisOptions = getChartOptionGroups();
    assert copyOptions.length == thisOptions.length;
    for (int i = 0; i < copyOptions.length; i++) {
      copyOptions[i].copyFrom(thisOptions[i]);
    }
    copy.calculateRowHeight();
  }

  @Override
  public OptionEventDispatcher getOptionEventDispatcher() {
    return myOptionEventDispatcher;
  }

  public class OptionEventDispatcher {
    void optionsChanged() {
      fireOptionsChanged();
    }
  }

  public ScrollingSession createScrollingSession(int startXpos) {
    assert myScrollingSession == null;
    return new ScrollingSessionImpl(startXpos);
  }

  public ChartItem getChartItemWithCoordinates(int x, int y) {
    GraphicPrimitiveContainer.GraphicPrimitive text = myTimelineLabelRenderer.getLabelLayer().getPrimitive(x, y);
    if (text instanceof GraphicPrimitiveContainer.Text) {
      return new TimelineLabelChartItem((Task)text.getModelObject());
    }
    return null;
  }

  @Override
  public Collection<Task> getTimelineTasks() {
    return Sets.union(myTimelineTasks, getMilestones());
  }

  private Set<Task> getMilestones() {
    return myTimelineMilestonesOption.getValue() ? Sets.filter(Sets.newHashSet(getTaskManager().getTasks()), MILESTONE_PREDICATE) : Collections.<Task>emptySet();
  }

  public void setTimelineTasks(Set<Task> timelineTasks) {
    myTimelineTasks = timelineTasks;
  }
}