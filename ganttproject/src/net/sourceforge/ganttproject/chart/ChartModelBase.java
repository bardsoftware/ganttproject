/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.GPOptionChangeListener;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitFunctionOfDate;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * Controls painting of the common part of Gantt and resource charts (in particular, timeline).
 * Calculates data required by the specific charts (e.g. calculates the offsets of the timeline
 * grid cells)
 */
public abstract class ChartModelBase implements /*TimeUnitStack.Listener,*/ ChartModel {
    public static interface ScrollingSession {
        void setXpos(int value);
        void finish();
    }

    private class ScrollingSessionImpl implements ScrollingSession {
        private int myPrevXpos;

        private List<Offset> myTopOffsets;
        private OffsetList myBottomOffsets;
        private List<Offset> myDefaultOffsets;

        private ScrollingSessionImpl(int startXpos) {
            //System.err.println("start xpos=" + startXpos);
            myPrevXpos = startXpos;
            ChartModelBase.this.myScrollingSession = this;
            ChartModelBase.this.constructOffsets();
            myTopOffsets = getTopUnitOffsets();
            myBottomOffsets = getBottomUnitOffsets();
            myDefaultOffsets = getDefaultUnitOffsets();
            //shiftOffsets(-myBottomOffsets.get(0).getOffsetPixels());
            //System.err.println(myBottomOffsets.subList(0, 3));
        }

        @Override
        public void setXpos(int xpos) {
            int shift = xpos - myPrevXpos;
            //System.err.println("xpos="+xpos+" shift=" + shift);
            shiftOffsets(shift);
            if (myBottomOffsets.get(0).getOffsetPixels() > 0) {
                int currentExceed = myBottomOffsets.get(0).getOffsetPixels();
                ChartModelBase.this.setStartDate(getBottomUnit().jumpLeft(getStartDate()));
                shiftOffsets(-myBottomOffsets.get(1).getOffsetPixels() + currentExceed);
                //System.err.println("one time unit to the left. start date=" + ChartModelBase.this.getStartDate());
                //System.err.println(myBottomOffsets.subList(0, 3));
            } else if (myBottomOffsets.get(1).getOffsetPixels() <= 0) {
                ChartModelBase.this.setStartDate(myBottomOffsets.get(2).getOffsetStart());
                shiftOffsets(-myBottomOffsets.get(0).getOffsetPixels());
                //System.err.println("one time unit to the right. start date=" + ChartModelBase.this.getStartDate());
                //System.err.println(myBottomOffsets.subList(0, 3));
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
            ChartModelBase.shiftOffsets(myBottomOffsets, shiftPixels);
            ChartModelBase.shiftOffsets(myTopOffsets, shiftPixels);
            if (myDefaultOffsets != myBottomOffsets) {
                if (myDefaultOffsets.isEmpty()) {
                    myDefaultOffsets = ChartModelBase.this.getDefaultUnitOffsets();
                }
                ChartModelBase.shiftOffsets(myDefaultOffsets, shiftPixels);
            }
            myBottomOffsets.setStartPx(myBottomOffsets.getStartPx() + shiftPixels);
        }
    }

    private static void shiftOffsets(List<Offset> offsets, int shiftPixels) {
        for (Offset o : offsets) {
            o.shift(shiftPixels);
        }
    }

    class OffsetBuilderImpl extends RegularFrameOffsetBuilder {
        private final boolean isCompressedWeekend;

        public OffsetBuilderImpl(ChartModelBase model, int width, Date endDate) {
            super(model.getTaskManager().getCalendar(),
                  model.getBottomUnit(),
                  model.getTimeUnitStack().getDefaultTimeUnit(),
                  model.getOffsetAnchorDate(),
                  model.getStartDate(),
                  model.getBottomUnitWidth(),
                  width,
                  model.getTopUnit().isConstructedFrom(model.getBottomUnit()) ?
                          RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR : 1f,
                  endDate,
                  0);
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

    public ChartModelBase(TaskManager taskManager, TimeUnitStack timeUnitStack,
            UIConfiguration projectConfig) {
        myTaskManager = taskManager;
        myProjectConfig = projectConfig;
        myChartUIConfiguration = new ChartUIConfiguration(projectConfig);
        myPainter = new StyledPainterImpl(myChartUIConfiguration);
        myTimeUnitStack = timeUnitStack;
        myChartHeader = new ChartHeaderImpl(this);
        myChartGrid = new ChartDayGridRenderer(this, projectConfig, myChartHeader.getTimelineContainer());
        myBackgroundRenderer = new BackgroundRendererImpl(this);
        addRenderer(myBackgroundRenderer);
        addRenderer(myChartHeader);
        addRenderer(myChartGrid);
    }

    private List<Offset> myTopUnitOffsets = new ArrayList<Offset>();
    private OffsetList myBottomUnitOffsets = new OffsetList();

    private List<Offset> myDefaultUnitOffsets = new ArrayList<Offset>();

    public List<Offset> getTopUnitOffsets() {
        return myTopUnitOffsets;
    }

    public OffsetList getBottomUnitOffsets() {
        return myBottomUnitOffsets;
    }

    public List<Offset> getDefaultUnitOffsets() {
        if (getBottomUnit().equals(getTimeUnitStack().getDefaultTimeUnit())) {
            return getBottomUnitOffsets();
        }
        if (myDefaultUnitOffsets.isEmpty()) {
            OffsetBuilderImpl offsetBuilder = new OffsetBuilderImpl(this, (int)getBounds().getWidth(), null);
            int defaultUnitCountPerLastBottomUnit = RegularFrameOffsetBuilder.getConcreteUnit(
                getBottomUnit(), getEndDate()).getAtomCount(getDefaultUnit());
            offsetBuilder.setRightMarginBottomUnitCount(myScrollingSession==null ? 0 : defaultUnitCountPerLastBottomUnit*2);
            offsetBuilder.constructBottomOffsets(myDefaultUnitOffsets, 0);
        }
        return myDefaultUnitOffsets;
    }

    Date getOffsetAnchorDate() {
        return /*myScrollingSession == null ?
            myStartDate :*/ getBottomUnit().jumpLeft(myStartDate);
    }

    private void constructOffsets() {
        myTopUnitOffsets.clear();
        myBottomUnitOffsets.clear();
        myDefaultUnitOffsets.clear();

        //System.err.println("offsets start date=" + startDate);
        OffsetBuilder offsetBuilder = createOffsetBuilderFactory().build();
        offsetBuilder.constructOffsets(myTopUnitOffsets, myBottomUnitOffsets);
    }

    public OffsetBuilder.Factory createOffsetBuilderFactory() {
        OffsetBuilder.Factory factory = new RegularFrameOffsetBuilder.FactoryImpl()
            .withAtomicUnitWidth(getBottomUnitWidth())
            .withBottomUnit(getBottomUnit())
            .withCalendar(myTaskManager.getCalendar())
            .withRightMargin(myScrollingSession == null ? 0 : 1)
            .withStartDate(getOffsetAnchorDate())
            .withViewportStartDate(getStartDate())
            .withTopUnit(myTopUnit)
            .withWeekendDecreaseFactor(getTopUnit().isConstructedFrom(getBottomUnit()) ?
                RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR : 1f);
        if(getBounds() != null ) {
            factory.withEndOffset((int) getBounds().getWidth());
        }
        return factory;
    }

    public void paint(Graphics g) {
        if (myScrollingSession == null
                && (myTopUnitOffsets.size() == 0 || myBottomUnitOffsets.size() == 0 || myDefaultUnitOffsets.size() == 0)) {
            constructOffsets();
        }
        int height = (int) getBounds().getHeight();
        for (ChartRendererBase renderer: getRenderers()) {
            renderer.clear();
            renderer.setHeight(height);
        }
        for (ChartRendererBase renderer: getRenderers()) {
            renderer.render();
        }
        myPainter.setGraphics(g);
        for (ChartRendererBase renderer: getRenderers()) {
            renderer.getPrimitiveContainer().paint(myPainter);
        }
        for (int layer = 0; ; layer++) {
            boolean layerPainted = false;
            for (ChartRendererBase renderer: getRenderers()) {
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

    public void addRenderer(ChartRendererBase renderer) {
        myRenderers.add(renderer);
    }

    protected Painter getPainter() {
        return myPainter;
    }

    public void resetRenderers() {
        myRenderers.clear();
    }

    public void setBounds(Dimension bounds) {
        myBounds = bounds;
    }

    public void setStartDate(Date startDate) {
        //System.err.println("ChartModelBase.setStartDate: " + startDate);
        myHorizontalOffset = 0;
        if (!startDate.equals(myStartDate)) {
            myStartDate = startDate;
            constructOffsets();
        }
    }

    public Date getStartDate() {
        return myStartDate;
    }

    public Date getEndDate() {
        List<Offset> offsets = getBottomUnitOffsets();
        return offsets.isEmpty() ? null : offsets.get(offsets.size()-1).getOffsetEnd();
    }

    public void setBottomUnitWidth(int pixelsWidth) {
        myAtomUnitPixels = pixelsWidth;
    }

    public void setRowHeight(int rowHeight) {
        getChartUIConfiguration().setRowHeight(rowHeight);
    }

    public void setTopTimeUnit(TimeUnit topTimeUnit) {
        setTopUnit(topTimeUnit);
    }

    public void setBottomTimeUnit(TimeUnit bottomTimeUnit) {
        myBottomUnit = bottomTimeUnit;
    }

    protected UIConfiguration getProjectConfig() {
        return myProjectConfig;
    }

    public Dimension getBounds() {
        return myBounds;
    }

    public Dimension getMaxBounds() {
        OffsetBuilderImpl offsetBuilder = new OffsetBuilderImpl(
                this, Integer.MAX_VALUE, getTaskManager().getProjectEnd());
        List<Offset> topUnitOffsets = new ArrayList<Offset>();
        OffsetList bottomUnitOffsets = new OffsetList();
        offsetBuilder.constructOffsets(topUnitOffsets, bottomUnitOffsets);
        int width = topUnitOffsets.get(topUnitOffsets.size()-1).getOffsetPixels();
        int height = calculateRowHeight()*getRowCount();
        return new Dimension(width, height);
    }

    public abstract int calculateRowHeight();
    protected abstract int getRowCount();


    public int getBottomUnitWidth() {
        return myAtomUnitPixels;
    }

    public TimeUnitStack getTimeUnitStack() {
        return myTimeUnitStack;
    }

    public ChartUIConfiguration getChartUIConfiguration() {
        return myChartUIConfiguration;
    }

    private void setChartUIConfiguration(ChartUIConfiguration chartConfig) {
        myChartUIConfiguration = chartConfig;
    }

    protected final TaskManager myTaskManager;

    private int myVerticalOffset;

    private int myHorizontalOffset;

    private ScrollingSessionImpl myScrollingSession;

    public TaskManager getTaskManager() {
        return myTaskManager;
    }

    public ChartHeader getChartHeader() {
        return myChartHeader;
    }

    public Offset getOffsetAt(int x) {
        //x = x + myHorizontalOffset;
       // System.err.println("x=" + x + " horoffset=" + myHorizontalOffset+ " offsets:\n" + getDefaultUnitOffsets());
//        OffsetLookup lookup = new OffsetLookup();
//        Date result = lookup.lookupDateByPixels(x, getDefaultUnitOffsets());
//        System.err.println("result=" + result);
//        return result;
        for (Offset offset : getDefaultUnitOffsets()) {
            if (offset.getOffsetPixels()>=x) {
                //System.err.println("result=" + offset);
                return offset;
            }
        }
        List<Offset> offsets = getBottomUnitOffsets();
        return offsets.get(offsets.size()-1);
    }

    /**
     * @return A length of the visible part of this chart area measured in the
     *         bottom line time units
     */
    public TaskLength getVisibleLength() {
        double pixelsLength = getBounds().getWidth();
        float unitsLength = (float) (pixelsLength / getBottomUnitWidth());
        TaskLength result = getTaskManager().createLength(getBottomUnit(),
                unitsLength);
        return result;
    }

    public void setHeaderHeight(int i) {
        getChartUIConfiguration().setHeaderHeight(i);
    }

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

    public TimeUnit getBottomUnit() {
        return myBottomUnit;
    }

    private TimeUnit getDefaultUnit() {
        return getTimeUnitStack().getDefaultTimeUnit();
    }

    private void setTopUnit(TimeUnit myTopUnit) {
        this.myTopUnit = myTopUnit;
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
            result = ((TimeUnitFunctionOfDate) myTopUnit)
                    .createTimeUnit(startDate);
        }
        return result;
    }

    public GPOptionGroup[] getChartOptionGroups() {
        return new GPOptionGroup[] {myChartGrid.getOptions()};
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
}