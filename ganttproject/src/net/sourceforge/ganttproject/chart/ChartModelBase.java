/*
 * This code is provided under the terms of GPL version 2.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2010
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
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

    class OffsetBuilderImpl extends RegularFrameOffsetBuilder {
        private final boolean isCompressedWeekend;

        public OffsetBuilderImpl(ChartModelBase model, int width, Date endDate) {
            super(model.getTaskManager().getCalendar(),
                  model.getBottomUnit(),
                  model.getTimeUnitStack().getDefaultTimeUnit(),
                  model.getStartDate(),
                  model.getBottomUnitWidth(),
                  width,
                  model.getTopUnit().isConstructedFrom(model.getBottomUnit()) ?
                          RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR : 1f,
                  endDate);
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

    private final ChartHeaderImpl myChartHeader;

    private Dimension myBounds;

    private Date myStartDate;

    protected int myAtomUnitPixels;

    protected final TimeUnitStack myTimeUnitStack;

    private TimeUnit myTopUnit;

    protected TimeUnit myBottomUnit;

    private final BackgroundRendererImpl myBackgroundRenderer;

    private final StyledPainterImpl myPainter;

    private final List<GPOptionChangeListener> myOptionListeners = new ArrayList<GPOptionChangeListener>();

    private final UIConfiguration myProjectConfig;

    private final Map<Range, List<Offset>> myRange2DefaultUnitOffsets = new HashMap<Range, List<Offset>>();

    private final List<ChartRendererBase> myRenderers = new ArrayList<ChartRendererBase>();

    public ChartModelBase(TaskManager taskManager, TimeUnitStack timeUnitStack,
            UIConfiguration projectConfig) {
        myTaskManager = taskManager;
        myProjectConfig = projectConfig;
        myChartUIConfiguration = new ChartUIConfiguration(projectConfig);
        myPainter = new StyledPainterImpl(myChartUIConfiguration);
        myTimeUnitStack = timeUnitStack;
        myChartHeader = new ChartHeaderImpl(this, projectConfig);
        myBackgroundRenderer = new BackgroundRendererImpl(this);
        addRenderer(myBackgroundRenderer);
        addRenderer(myChartHeader);
    }

    private List<Offset> myTopUnitOffsets = new ArrayList<Offset>();
    private List<Offset> myBottomUnitOffsets = new ArrayList<Offset>();

    private List<Offset> myDefaultUnitOffsets = new ArrayList<Offset>();

    private int myShiftPixels;

    public List<Offset> getTopUnitOffsets() {
        return myTopUnitOffsets;
    }

    public List<Offset> getBottomUnitOffsets() {
        return myBottomUnitOffsets;
    }

    public List<Offset> getDefaultUnitOffsets() {
        if (getBottomUnit().equals(getTimeUnitStack().getDefaultTimeUnit())) {
            return getBottomUnitOffsets();
        }
        if (myDefaultUnitOffsets.isEmpty()) {
            OffsetBuilderImpl offsetBuilder = new OffsetBuilderImpl(this, (int)getBounds().getWidth(), null);
            offsetBuilder.constructBottomOffsets(myDefaultUnitOffsets, 0);
            shiftOffsets(myDefaultUnitOffsets, myShiftPixels);
        }
        return myDefaultUnitOffsets;
    }

    class Range {
        Offset start;
        Offset end;
        public Range(Offset startOffset, Offset endOffset) {
            start = startOffset;
            end = endOffset;
        }
        @Override
        public boolean equals(Object that) {
            if (false == that instanceof Range) {
                return false;
            }
            Range thatRange = (Range) that;
            return (this.start == null ? thatRange.start == null : this.start.equals(thatRange.start))
                    && thatRange.end.equals(this.end);
        }
        @Override
        public int hashCode() {
            return ((this.start == null ? 0 : 7 * this.start.hashCode()) + 11 * this.end.hashCode()) / 13;
        }
    }

    protected void constructOffsets() {
        myTopUnitOffsets.clear();
        myBottomUnitOffsets.clear();
        myDefaultUnitOffsets.clear();

        Date startDate = (myHorizontalOffset > 0) ?
            getBottomUnit().jumpLeft(myStartDate) : myStartDate;
        RegularFrameOffsetBuilder offsetBuilder = new RegularFrameOffsetBuilder(
            myTaskManager.getCalendar(), myTopUnit, getBottomUnit(), startDate,
            getBottomUnitWidth(), (int)getBounds().getWidth() + getBottomUnitWidth()*2,
            getTopUnit().isConstructedFrom(getBottomUnit()) ?
                RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR : 1f);
        offsetBuilder.constructOffsets(myTopUnitOffsets, myBottomUnitOffsets);
        //System.err.println("startDate=" + startDate);
        myShiftPixels = (myHorizontalOffset <= 0) ? 
            myHorizontalOffset : myHorizontalOffset - myBottomUnitOffsets.get(1).getOffsetPixels();
        shiftOffsets(myBottomUnitOffsets, myShiftPixels);
        shiftOffsets(myTopUnitOffsets, myShiftPixels);
    }

    private void shiftOffsets(List<Offset> offsets, int shiftPixels) {
        //System.err.println(" shift pixels=" + shiftPixels);
        for (Offset o : offsets) {
            o.shift(shiftPixels);
        }        
    }
    public void paint(Graphics g) {
        constructOffsets();
        int height = (int) getBounds().getHeight();
        for (ChartRendererBase renderer: getRenderers()) {
            renderer.clear();
            renderer.setHeight(height);
        }
        for (ChartRendererBase renderer: getRenderers()) {
            renderer.render();
        }
        //if (myHorizontalOffset != 0) {
  //      System.err.println("will shift:"+(myHorizontalOffset - getBottomUnitOffsets().get(1).getOffsetPixels()));
    //        System.err.println("offset="+getBottomUnitOffsets().get(1));
//            g.translate(myHorizontalOffset - getBottomUnitOffsets().get(1).getOffsetPixels(), 0);
            //g.translate(myHorizontalOffset, 0);
        //}
        myPainter.setGraphics(g);
        for (ChartRendererBase renderer: getRenderers()) {
            renderer.getPrimitiveContainer().paint(myPainter, g);
        }
        for (int layer = 0; ; layer++) {
            boolean layerPainted = false;
            for (ChartRendererBase renderer: getRenderers()) {
                List<GraphicPrimitiveContainer> layers = renderer.getPrimitiveContainer().getLayers();
                if (layer < layers.size()) {
                    layers.get(layer).paint(myPainter, g);
                    layerPainted = true;
                }
            }
            if (!layerPainted) {
                break;
            }
        }
    }

    private List<ChartRendererBase> getRenderers() {
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
        }
        myRange2DefaultUnitOffsets.clear();
        if (myBounds!=null) {
            constructOffsets();
        }
    }

    public Date getStartDate() {
        return myStartDate;
    }

    public Date getEndDate() {
        List<Offset> offsets = getBottomUnitOffsets();
        return offsets.get(offsets.size()-1).getOffsetEnd();
//        for (int i = offsets.size()-1; i>=0; i--) {
//            if (offsets.get(i).getOffsetPixels()>getBounds().getWidth()) {
//                lastOutOfBounds = offsets.get(i);
//            }
//            else {
//                return lastOutOfBounds.getOffsetEnd();
//            }
//        }
//        throw new IllegalStateException();
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
        List<Offset> bottomUnitOffsets = new ArrayList<Offset>();
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

    protected final ChartUIConfiguration myChartUIConfiguration;

    public ChartUIConfiguration getChartUIConfiguration() {
        return myChartUIConfiguration;
    }

    protected final TaskManager myTaskManager;

    private int myVerticalOffset;

    private int myHorizontalOffset;

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

    public float calculateLength(int fromX, int toX, int y) {
        // return toX - fromX;

        int curX = fromX;
        int totalPixels = toX - fromX;
        int holidayPixels = 0;
        while (curX < toX) {
            GraphicPrimitiveContainer.GraphicPrimitive nextPrimitive = myChartHeader
                    .getPrimitiveContainer().getPrimitive(curX,
                            y - getChartUIConfiguration().getHeaderHeight());
            if (nextPrimitive instanceof GraphicPrimitiveContainer.Rectangle
                    && GPCalendar.DayType.WEEKEND == nextPrimitive
                            .getModelObject()) {
                GraphicPrimitiveContainer.Rectangle nextRect = (Rectangle) nextPrimitive;
                holidayPixels += nextRect.getRightX() - curX;
                if (nextRect.myLeftX < curX) {
                    holidayPixels -= curX - nextRect.myLeftX;
                }
                if (nextRect.myLeftX < fromX) {
                    holidayPixels -= fromX - nextRect.myLeftX;
                }
                if (nextRect.getRightX() > toX) {
                    holidayPixels -= nextRect.getRightX() - toX;
                }
                curX = nextRect.getRightX() + 1;
            } else {
                curX += getBottomUnitWidth();
            }
        }
        float workPixels = (float) totalPixels - (float) holidayPixels;
        return workPixels / (float) getBottomUnitWidth();
    }

    public float calculateLengthNoWeekends(int fromX, int toX) {
        int totalPixels = toX - fromX;
        return totalPixels / (float) getBottomUnitWidth();
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
        //myHorizontalOffset = pixels;
    }

    protected int getHorizontalOffset() {
        return myHorizontalOffset;
    }
    
    public TimeUnit getBottomUnit() {
        return myBottomUnit;
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
            } else {
                result = ((TimeUnitFunctionOfDate) myTopUnit)
                        .createTimeUnit(startDate);
            }
        }
        return result;
    }

    public GPOptionGroup[] getChartOptionGroups() {
        return new GPOptionGroup[] {myChartHeader.getOptions()};
    }

    public void addOptionChangeListener(GPOptionChangeListener listener) {
        myOptionListeners.add(listener);
    }

    protected void fireOptionsChanged() {
        for (int i = 0; i < myOptionListeners.size(); i++) {
            GPOptionChangeListener next = myOptionListeners
                    .get(i);
            next.optionsChanged();
        }
    }

    public abstract ChartModelBase createCopy();

    protected void setupCopy(ChartModelBase copy) {
        copy.setTopTimeUnit(getTopUnit());
        copy.setBottomTimeUnit(getBottomUnit());
        copy.setBottomUnitWidth(getBottomUnitWidth());
        copy.setStartDate(getStartDate());
    }

    public OptionEventDispatcher getOptionEventDispatcher() {
        return myOptionEventDispatcher;
    }

    public class OptionEventDispatcher {
        void optionsChanged() {
            fireOptionsChanged();
        }
    }

    public static class Offset {
        private Date myOffsetAnchor;
        private Date myOffsetEnd;
        private int myOffsetPixels;
        private TimeUnit myOffsetUnit;
        private GPCalendar.DayType myDayType;
        private Date myOffsetStart;

        Offset(TimeUnit offsetUnit, Date offsetAnchor, Date offsetStart, Date offsetEnd, int offsetPixels, GPCalendar.DayType dayType) {
            myOffsetAnchor = offsetAnchor;
            myOffsetStart = offsetStart;
            myOffsetEnd = offsetEnd;
            myOffsetPixels = offsetPixels;
            myOffsetUnit = offsetUnit;
            myDayType = dayType;
        }
        Date getOffsetAnchor() {
            return myOffsetAnchor;
        }
        public Date getOffsetStart() {
            return myOffsetStart;
        }
        public Date getOffsetEnd() {
            return myOffsetEnd;
        }
        public int getOffsetPixels() {
            return myOffsetPixels;
        }
        void shift(int pixels) {
            myOffsetPixels += pixels;
        }
        TimeUnit getOffsetUnit() {
            return myOffsetUnit;
        }
        public DayType getDayType() {
            return myDayType;
        }
        public String toString() {
            return "end date: " + myOffsetEnd + " end pixel: " + myOffsetPixels+" time unit: "+myOffsetUnit.getName();
        }
        @Override
        public boolean equals(Object that) {
            if (false==that instanceof Offset) {
                return false;
            }
            Offset thatOffset = (Offset) that;
            return myOffsetPixels==thatOffset.myOffsetPixels &&
                   myOffsetEnd.equals(thatOffset.myOffsetEnd) &&
                   myOffsetAnchor.equals(thatOffset.myOffsetAnchor);
        }
        @Override
        public int hashCode() {
            return myOffsetEnd.hashCode();
        }
    }
}