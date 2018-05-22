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

import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Painter;
import biz.ganttproject.core.chart.grid.*;
import biz.ganttproject.core.chart.grid.OffsetManager.OffsetBuilderFactory;
import biz.ganttproject.core.chart.render.TextLengthCalculatorImpl;
import biz.ganttproject.core.chart.scene.DayGridSceneBuilder;
import biz.ganttproject.core.chart.scene.SceneBuilder;
import biz.ganttproject.core.chart.scene.TimelineSceneBuilder;
import biz.ganttproject.core.chart.text.TimeFormatter;
import biz.ganttproject.core.chart.text.TimeFormatters;
import biz.ganttproject.core.chart.text.TimeUnitText.Position;
import biz.ganttproject.core.option.*;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.TimeUnitFunctionOfDate;
import biz.ganttproject.core.time.TimeUnitStack;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.sourceforge.ganttproject.chart.item.CalendarChartItem;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TimelineLabelChartItem;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.util.*;
import java.util.List;

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
      myTopOffsets = getTopUnitOffsets();
      myBottomOffsets = getBottomUnitOffsets();
      myDefaultOffsets = getDefaultUnitOffsets();
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
          myDefaultOffsets = ChartModelBase.this.getDefaultUnitOffsets();
        }
        myDefaultOffsets.shift(shiftPixels);
      }
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

  private final TimelineSceneBuilder myChartHeader;
  private final BackgroundRendererImpl myBackgroundRenderer;

  private final StyledPainterImpl myPainter;

  private final List<GPOptionChangeListener> myOptionListeners = new ArrayList<GPOptionChangeListener>();

  private final UIConfiguration myProjectConfig;
  private ChartUIConfiguration myChartUIConfiguration;

  private final List<SceneBuilder> myRenderers = Lists.newArrayList();

  private final DayGridSceneBuilder myChartGrid;

  private final TimelineLabelRendererImpl myTimelineLabelRenderer;

  protected final TaskManager myTaskManager;

  private int myVerticalOffset;

  private int myHorizontalOffset;

  private ScrollingSessionImpl myScrollingSession;

  private Set<Task> myTimelineTasks = Collections.emptySet();

  private final ChartOptionGroup myChartGridOptions;
  private final GPOptionGroup myTimelineLabelOptions;

  private final BooleanOption myTimelineMilestonesOption = new DefaultBooleanOption("timeline.showMilestones", true);
  private final FontOption myChartFontOption;

  public ChartModelBase(TaskManager taskManager, TimeUnitStack timeUnitStack, final UIConfiguration projectConfig) {
    myTaskManager = taskManager;
    myProjectConfig = projectConfig;
    myChartUIConfiguration = new ChartUIConfiguration(projectConfig);
    myChartFontOption = projectConfig.getChartFontOption();
    myPainter = new StyledPainterImpl(myChartUIConfiguration);
    myTimeUnitStack = timeUnitStack;
    final TimeFormatters.LocaleApi localeApi = new TimeFormatters.LocaleApi() {
      @Override
      public Locale getLocale() {
        return GanttLanguage.getInstance().getDateFormatLocale();
      }
      @Override
      public DateFormat createDateFormat(String pattern) {
        return GanttLanguage.getInstance().createDateFormat(pattern);
      }
      @Override
      public DateFormat getShortDateFormat() {
        return GanttLanguage.getInstance().getShortDateFormat();
      }
      @Override
      public String i18n(String key) {
        return GanttLanguage.getInstance().getText(key);
      }
    };
    final TimeFormatters timeFormatters = new TimeFormatters(localeApi);
    GanttLanguage.getInstance().addListener(new GanttLanguage.Listener() {
      @Override
      public void languageChanged(Event event) {
        timeFormatters.setLocaleApi(localeApi);
      }
    });
    myChartHeader = new TimelineSceneBuilder(new TimelineSceneBuilder.InputApi() {
      @Override
      public Date getViewportStartDate() {
        return getStartDate();
      }
      @Override
      public OffsetList getTopUnitOffsets() {
        return ChartModelBase.this.getTopUnitOffsets();
      }
      @Override
      public int getTopLineHeight() {
        return getChartUIConfiguration().getSpanningHeaderHeight();
      }
      @Override
      public int getTimelineHeight() {
        return getChartUIConfiguration().getHeaderHeight();
      }
      @Override
      public Color getTimelineBorderColor() {
        return getChartUIConfiguration().getHeaderBorderColor();
      }
      @Override
      public Color getTimelineBackgroundColor() {
        return getChartUIConfiguration().getSpanningHeaderBackgroundColor();
      }
      @Override
      public int getViewportWidth() {
        return getBounds().width;
      }
      @Override
      public OffsetList getBottomUnitOffsets() {
        return ChartModelBase.this.getBottomUnitOffsets();
      }
      @Override
      public TimeFormatter getFormatter(TimeUnit timeUnit, Position position) {
        return timeFormatters.getFormatter(timeUnit, position);
      }
    });
    myChartGridOptions = new ChartOptionGroup("ganttChartGridDetails",
        new GPOption[] { projectConfig.getRedlineOption(), projectConfig.getProjectBoundariesOption(), projectConfig.getWeekendAlphaRenderingOption(),
          myChartUIConfiguration.getChartStylesOption()},
        getOptionEventDispatcher());
    myChartGrid = new DayGridSceneBuilder(new DayGridSceneBuilder.InputApi() {
      @Override
      public Color getWeekendColor() {
        return myChartUIConfiguration.getHolidayTimeBackgroundColor();
      }
      @Override
      public Color getHolidayColor(Date holiday) {
        CalendarEvent event = getTaskManager().getCalendar().getEvent(holiday);
        if (event == null || event.getColor() == null) {
          return null;
        }
        return event.getColor();
      }

      public CalendarEvent getEvent(Date date) {
        return getTaskManager().getCalendar().getEvent(date);
      }
      @Override
      public int getTopLineHeight() {
        return myChartUIConfiguration.getSpanningHeaderHeight();
      }
      @Override
      public BooleanOption getRedlineOption() {
        return projectConfig.getRedlineOption();
      }
      @Override
      public Date getProjectStart() {
        return getTaskManager().getProjectStart();
      }
      @Override
      public Date getProjectEnd() {
        return getTaskManager().getProjectEnd();
      }
      @Override
      public BooleanOption getProjectDatesOption() {
        return projectConfig.getProjectBoundariesOption();
      }
      @Override
      public OffsetList getAtomUnitOffsets() {
        return getDefaultUnitOffsets();
      }
    }, myChartHeader.getTimelineContainer());
    myBackgroundRenderer = new BackgroundRendererImpl(this);
    myTimelineLabelOptions = new ChartOptionGroup("timelineLabels", new GPOption[] { myTimelineMilestonesOption }, getOptionEventDispatcher());
    myTimelineLabelRenderer = new TimelineLabelRendererImpl(this);
    addRenderer(myBackgroundRenderer);
    addRenderer(myChartHeader);
    addRenderer(myChartGrid);
    addRenderer(myTimelineLabelRenderer);

    ChangeValueListener fontChangeValueListener = new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        setBaseFont(myChartFontOption.getValue());
      }
    };
    myChartFontOption.addChangeValueListener(fontChangeValueListener);
    getProjectConfig().getDpiOption().addChangeValueListener(fontChangeValueListener);
    setBaseFont(myChartFontOption.getValue());
  }

  private void setBaseFont(FontSpec fontSpec) {
    float scaleFactor = fontSpec.getSize().getFactor();
    if (getProjectConfig().getDpiOption() != null) {
      scaleFactor *= getProjectConfig().getDpiOption().getValue().floatValue() / UIFacade.DEFAULT_DPI;
    }
    Font font = new Font(fontSpec.getFamily(), Font.PLAIN, (int)(10*scaleFactor));
    BufferedImage dummyImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_BGR);
    Graphics2D g = (Graphics2D) dummyImage.getGraphics();
    TextLengthCalculatorImpl calculator = new TextLengthCalculatorImpl(g);
    int fontSize = calculator.getTextHeight(font, "Agpqf");
    getChartUIConfiguration().setBaseFont(font, fontSize);
  }

  private OffsetManager myOffsetManager = new OffsetManager(new OffsetBuilderFactory() {
    @Override
    public OffsetBuilder createTopAndBottomUnitBuilder() {
      return createOffsetBuilderFactory().build();
    }

    @Override
    public OffsetBuilder createAtomUnitBuilder() {
      int defaultUnitCountPerLastBottomUnit = OffsetBuilderImpl.getConcreteUnit(
          getBottomUnit(), getEndDate()).getAtomCount(getDefaultUnit());
      return createOffsetBuilderFactory()
        .withRightMargin(myScrollingSession == null ? 0 : defaultUnitCountPerLastBottomUnit * 2)
        .withTopUnit(getBottomUnit())
        .withBottomUnit(getTimeUnitStack().getDefaultTimeUnit())
        .withOffsetStepFunction(new Function<TimeUnit, Float>() {
          @Override
          public Float apply(TimeUnit timeUnit) {
            int offsetUnitCount = timeUnit.getAtomCount(getTimeUnitStack().getDefaultTimeUnit());
            return 1f / offsetUnitCount;
          }
        }).build();
    }
  });

  @Override
  public void resetOffsets() {
    myOffsetManager.reset();
  }

  @Override
  public OffsetList getTopUnitOffsets() {
    return myOffsetManager.getTopUnitOffsets();
  }

  @Override
  public OffsetList getBottomUnitOffsets() {
    return myOffsetManager.getBottomUnitOffsets();
  }

  @Override
  public OffsetList getDefaultUnitOffsets() {
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
    OffsetBuilder.Factory factory = new OffsetBuilderImpl.FactoryImpl()
      .withAtomicUnitWidth(getBottomUnitWidth())
      .withBottomUnit(getBottomUnit())
      .withCalendar(myTaskManager.getCalendar())
      .withRightMargin(myScrollingSession == null ? 0 : 1)
      .withStartDate(getOffsetAnchorDate())
      .withViewportStartDate(getStartDate())
      .withTopUnit(myTopUnit)
      .withWeekendDecreaseFactor(
          getTopUnit().isConstructedFrom(getBottomUnit()) ? OffsetBuilderImpl.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR : 1f);
    if (getBounds() != null) {
      factory.withEndOffset((int) getBounds().getWidth());
    }
    return factory;
  }

  @Override
  public void paint(Graphics g) {
    int height = (int) getBounds().getHeight();
    for (SceneBuilder renderer : getRenderers()) {
      renderer.reset(height);
    }
    for (SceneBuilder renderer : getRenderers()) {
      renderer.build();
    }
    myPainter.setGraphics(g);
    for (SceneBuilder renderer : getRenderers()) {
      renderer.getCanvas().paint(myPainter);
    }
    for (int layer = 0;; layer++) {
      boolean layerPainted = false;
      for (SceneBuilder renderer : getRenderers()) {
        List<Canvas> layers = renderer.getCanvas().getLayers();
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

  protected List<SceneBuilder> getRenderers() {
    return myRenderers;
  }

  @Override
  public void addRenderer(SceneBuilder renderer) {
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
    return new GPOptionGroup[] { myChartGridOptions, myTimelineLabelOptions };
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
    copy.setTimelineTasks(myTimelineTasks);
    copy.myTimelineMilestonesOption.setValue(myTimelineMilestonesOption.getValue());
    GPOptionGroup[] copyOptions = copy.getChartOptionGroups();
    GPOptionGroup[] thisOptions = getChartOptionGroups();
    assert copyOptions.length == thisOptions.length;
    for (int i = 0; i < copyOptions.length; i++) {
      copyOptions[i].copyFrom(thisOptions[i]);
    }
    copy.myChartFontOption.setValue(myChartFontOption.getValue());
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
    if (myScrollingSession != null) {
      myScrollingSession.finish();
    }
    return new ScrollingSessionImpl(startXpos);
  }

  public ChartItem getChartItemWithCoordinates(int x, int y) {
    Canvas.Shape text = myTimelineLabelRenderer.getLabelLayer().getPrimitive(x, y);
    if (text instanceof Canvas.Text) {
      return new TimelineLabelChartItem((Task)text.getModelObject());
    }
    Offset offset = getOffsetAt(x);
    if (offset != null) {
      return new CalendarChartItem(offset.getOffsetStart());
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
