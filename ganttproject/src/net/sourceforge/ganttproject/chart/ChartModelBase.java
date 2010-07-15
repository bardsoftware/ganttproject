package net.sourceforge.ganttproject.chart;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.calendar.CalendarFactory;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.GPOptionChangeListener;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.zoom.ZoomEvent;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitFunctionOfDate;
import net.sourceforge.ganttproject.time.TimeUnitStack;

public abstract class ChartModelBase implements ChartViewState.Listener {
    class OffsetBuilderImpl extends RegularFrameOffsetBuilder {
        private final boolean isCompressedWeekend;

        public OffsetBuilderImpl(ChartModelBase model, int width, Date endDate) {
            super(model.getTimeUnitStack(), 
                  model.getTaskManager().getCalendar(), 
                  model.getBottomUnit(), 
                  model.getTimeUnitStack().getDefaultTimeUnit(), 
                  model.getStartDate(), 
                  model.getBottomUnitWidth(), 
                  width,
                  model.myTopUnit.isConstructedFrom(model.getBottomUnit()) ? 
                          RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR : 1f,
                  endDate);
            isCompressedWeekend = model.getTopUnit().isConstructedFrom(model.getBottomUnit());
        }
//        protected void calculateNextStep(OffsetStep step, TimeFrame currentFrame, Date startDate) {
//            float offsetStep = getOffsetStep(currentFrame);
//            boolean isBottomUnitWorking = ChartModelBase.this.getBottomUnit().isWorkingInterval(startDate);
//            if (!isBottomUnitWorking) {
//                step.dayType = GPCalendar.DayType.WEEKEND;
////                step.incrementTopUnit = false;
//                if (isCompressedWeekend) {
//                    offsetStep = offsetStep / RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR;
//                }
//            }
//            else {
//                step.dayType = GPCalendar.DayType.WORKING;
//            }
//            step.parrots += offsetStep;
//        }
        protected float getOffsetStep(TimeFrame timeFrame) {
            int offsetUnitCount = timeFrame.getUnitCount(getTimeUnitStack().getDefaultTimeUnit());
            return 1f / offsetUnitCount;
        }
    }

    public static final Object STATIC_MUTEX = new Object();

    private final OptionEventDispatcher myOptionEventDispatcher = new OptionEventDispatcher();

    private final ChartHeaderImpl myChartHeader;

    private final ChartGridImpl myChartGrid;

    private Dimension myBounds;

    private Date myStartDate;

    protected int myAtomUnitPixels;

    private TimeFrame[] myTimeFrames;

    protected final TimeUnitStack myTimeUnitStack;

    private TimeUnit myTopUnit;

    protected TimeUnit myBottomUnit;

    protected java.util.List myTimeUnitVisitors = new ArrayList();

    //protected final BottomUnitLineRendererImpl myBottomUnitLineRenderer;

    protected TimeFrameWidthFunction myFrameWidthFunction;

    protected RegularFramesWithFunction myRegularFrameWidthFunction = new RegularFramesWithFunction();

    protected SkewedFramesWidthFunction mySkewedFrameWidthFunction = new SkewedFramesWidthFunction();

    private final BackgroundRendererImpl myBackgroundRenderer;

    private final StyledPainterImpl myPainter;

    private final List myOptionListeners = new ArrayList();

    private final UIConfiguration myProjectConfig;

	private CachingOffsetCalculatorImpl myCachingOffsetCalculator;

    public ChartModelBase(TaskManager taskManager, TimeUnitStack timeUnitStack,
            UIConfiguration projectConfig) {
        myTaskManager = taskManager;
        myProjectConfig = projectConfig;
        myChartUIConfiguration = new ChartUIConfiguration(projectConfig);
        myPainter = new StyledPainterImpl(myChartUIConfiguration);
        myTimeUnitStack = timeUnitStack;
        myChartHeader = new ChartHeaderImpl(this, projectConfig);
        myChartGrid = new ChartGridImpl(this, projectConfig);
        //myBottomUnitLineRenderer = new BottomUnitLineRendererImpl(this);
        myBackgroundRenderer = new BackgroundRendererImpl(this);
        myTimeUnitVisitors.add(myChartGrid);
        //myTimeUnitVisitors.add(myBottomUnitLineRenderer);
        myCachingOffsetCalculator = new CachingOffsetCalculatorImpl(myTimeUnitStack);
    }

    private List myTopUnitOffsets = new ArrayList();
    private List myBottomUnitOffsets = new ArrayList();

    private List myDefaultUnitOffsets = new ArrayList();

    List getTopUnitOffsets() {
        return myTopUnitOffsets;
    }

    List getBottomUnitOffsets() {
        return myBottomUnitOffsets;
    }

    List getDefaultUnitOffsets() {
        if (myDefaultUnitOffsets.isEmpty()) {
            ArrayList tmpOffsets = new ArrayList();
            OffsetBuilderImpl offsetBuilder = new OffsetBuilderImpl(this, (int)getBounds().getWidth(), null);
//            RegularFrameOffsetBuilder offsetBuilder = new RegularFrameOffsetBuilder(
//                    getTimeUnitStack(),
//                    myTaskManager.getCalendar(),
//                    getBottomUnit(),
//                    getTimeUnitStack().getDefaultTimeUnit(),
//                    myStartDate, getBottomUnitWidth(),
//                    (int)getBounds().getWidth(),
//                    RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR) {
//                protected float getOffsetStep(TimeFrame timeFrame) {
//                    int offsetUnitCount = timeFrame.getUnitCount(getTimeUnitStack().getDefaultTimeUnit());
//                    return 1f / offsetUnitCount;
//                }
//            };
            offsetBuilder.constructOffsets(tmpOffsets, myDefaultUnitOffsets);
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
        public boolean equals(Object that) {
            if (false==that instanceof Range) {
                return false;
            }
            Range thatRange = (Range) that;
            return (this.start==null ? thatRange.start==null : this.start.equals(thatRange.start)) && thatRange.end.equals(this.end);
        }
        public int hashCode() {
            return ((this.start==null ? 0:7*this.start.hashCode()) + 11*this.end.hashCode()) / 13;
        }


    }
    Map/*<Range, List<Offset>>*/ myRange2DefaultUnitOffsets = new HashMap/*<Range, List<Offset>>*/();

    List/*<Offset>*/ getDefaultUnitOffsetsInRange(Offset startOffset, Offset endOffset) {
        Range range = new Range(startOffset, endOffset);
        List/*<Offset>*/ result = (List) myRange2DefaultUnitOffsets.get(range);
        if (result!=null) {
            return result;
        }
        ArrayList/*<Offset>*/ tmpOffsets = new ArrayList/*<Offset>*/();
        result = new ArrayList/*<Offset>*/();

        int initialEnd = startOffset==null ? 0 : startOffset.getOffsetPixels();
        Date startDate = startOffset==null ? myStartDate : startOffset.getOffsetEnd();
        RegularFrameOffsetBuilder offsetBuilder = new RegularFrameOffsetBuilder(
                getTimeUnitStack(),
                myTaskManager.getCalendar(),
                endOffset.getOffsetUnit(),
                getTimeUnitStack().getDefaultTimeUnit(),
                startDate,
                getBottomUnitWidth(),
                endOffset.getOffsetPixels(),
                1f, null) {
            protected float getOffsetStep(TimeFrame timeFrame) {
                int offsetUnitCount = timeFrame.getUnitCount(getTimeUnitStack().getDefaultTimeUnit());
                return 1f / offsetUnitCount;
            }
        };
        offsetBuilder.constructOffsets(tmpOffsets, result, initialEnd);
        myRange2DefaultUnitOffsets.put(range, result);
//        System.err.println("For start offstet["+startOffset+"] end offset["+endOffset+"]");
//        for (Offset t : result) {
//            System.err.println(t);
//        }
        return result;
    }
    public void paint(Graphics g) {
        int height = (int) getBounds().getHeight()
                - getChartUIConfiguration().getHeaderHeight();
        myTopUnitOffsets.clear();
        myBottomUnitOffsets.clear();
        myDefaultUnitOffsets.clear();

        if (getTopUnit().isConstructedFrom(getBottomUnit())) {
            // It is the case when top unit can always be constructed from integer number of bottom units,
            // and bounds of top unit and start/end bottom units are aligned
            // e.g. week can be constructed from days
            RegularFrameOffsetBuilder offsetBuilder = new RegularFrameOffsetBuilder(
                    myTimeUnitStack, myTaskManager.getCalendar(), myTopUnit, getBottomUnit(), myStartDate,
                    getBottomUnitWidth(), (int)getBounds().getWidth(), RegularFrameOffsetBuilder.WEEKEND_UNIT_WIDTH_DECREASE_FACTOR, null);
            offsetBuilder.constructOffsets(myTopUnitOffsets, myBottomUnitOffsets);
        }
        else {
            // In this case top unit can't be constructed from integer number of bottom units
            // and unit boundaries aren't aligned. Thus, month and weeks are skewed units
            SkewedFrameOffsetBuilder offsetBuilder = new SkewedFrameOffsetBuilder(
                    myTimeUnitStack, myTaskManager.getCalendar(), myTopUnit, getBottomUnit(), myStartDate,
                    getBottomUnitWidth(), (int)getBounds().getWidth());
            offsetBuilder.constructOffsets(myTopUnitOffsets, myBottomUnitOffsets);
        }
        myChartHeader.setHeight(height);
        myChartHeader.render();
        myChartGrid.setHeight(height);
        myBackgroundRenderer.setHeight(height);
        myPainter.setGraphics(g);
        myBackgroundRenderer.getPrimitiveContainer().paint(getPainter(), g);
        myChartHeader.getPrimitiveContainer().paint(getPainter(), g);
    }

    protected StyledPainterImpl getPainter() {
        return myPainter;
    }

    protected void enableRenderers1() {
        myChartHeader.setEnabled(false);
        //myBottomUnitLineRenderer.setEnabled(true);
        myChartGrid.setEnabled(true);
    }

    protected void enableRenderers2() {
        myChartHeader.setEnabled(true);
        //myBottomUnitLineRenderer.setEnabled(false);
        myChartGrid.setEnabled(false);
    }

    protected void paintSkewedTimeFrames(Graphics g) {
        TimeUnit savedBottomUnit = myBottomUnit;
        TimeUnit topUnit = getTopUnit();
        setTopUnit(myBottomUnit);
        myTimeFrames = null;
        enableRenderers1();
        TimeFrame[] timeFrames = getTimeFrames(null);
        paintRegularTimeFrames(g, timeFrames);
        Date exactStart = timeFrames[0].getStartDate();
        // System.err.println("... done");
        // System.err.println("[ChartModelImpl] rendering skewed frames. Top
        // unit="+myTopUnit+" bottom unit="+myBottomUnit);
        // System.err.println(" rendering top line");
        myTimeFrames = null;
        setTopUnit(topUnit);
        myBottomUnit = topUnit;
        enableRenderers2();
        timeFrames = getTimeFrames(exactStart);

        paintRegularTimeFrames(g, timeFrames);
        myBottomUnit = savedBottomUnit;
        //
        // System.err.println(" rendering bottom line");
    }

    protected void paintMainArea(Graphics mainArea, Painter p) {
        myChartGrid.getPrimitiveContainer().paint(p, mainArea);
    }

    protected void paintRegularTimeFrames(Graphics g, TimeFrame[] timeFrames) {
        fireBeforeProcessingTimeFrames();
        for (int i = 0; i < timeFrames.length; i++) {
            TimeFrame next = timeFrames[i];
            fireFrameStarted(next);
            TimeUnit topUnit = next.getTopUnit();
            fireUnitLineStarted(topUnit);
            fireUnitLineFinished(topUnit);
            //
            TimeUnit bottomUnit = myBottomUnit;// next.getBottomUnit();
            fireUnitLineStarted(bottomUnit);
            visitTimeUnits(next, bottomUnit);
            fireUnitLineFinished(bottomUnit);
            fireFrameFinished(next);
        }
        fireAfterProcessingTimeFrames();
        // Painter p = new StyledPainterImpl(g, getChartUIConfiguration());
        myChartHeader.getPrimitiveContainer().paint(myPainter, g);
        //myBottomUnitLineRenderer.getPrimitiveContainer().paint(myPainter, g);
        Graphics mainArea = g.create(0, getChartUIConfiguration()
                .getHeaderHeight(), (int) getBounds().getWidth(),
                (int) getBounds().getHeight());
        myPainter.setGraphics(mainArea);
        // p = new StyledPainterImpl(mainArea, getChartUIConfiguration());
        myBackgroundRenderer.getPrimitiveContainer().paint(myPainter, g);
        paintMainArea(mainArea, myPainter);
        // Graphics resourcesArea = g.create((int)getBounds().getWidth()-20,
        // getChartUIConfiguration().getHeaderHeight(), 20,
        // (int)getBounds().getHeight());
        // myResourcesRendererImpl.getPrimitiveContainer().paint(p,
        // resourcesArea);
        // myTaskProgressRendererImpl.getPrimitiveContainer().paint(p,
        // mainArea);
    }

    void fireBeforeProcessingTimeFrames() {
        myBackgroundRenderer.beforeProcessingTimeFrames();
        for (int i = 0; i < myTimeUnitVisitors.size(); i++) {
            TimeUnitVisitor nextVisitor = (TimeUnitVisitor) myTimeUnitVisitors
                    .get(i);
            if (!nextVisitor.isEnabled()) {
                continue;
            }
            nextVisitor.beforeProcessingTimeFrames();
        }
    }

    void fireAfterProcessingTimeFrames() {
        for (int i = 0; i < myTimeUnitVisitors.size(); i++) {
            TimeUnitVisitor nextVisitor = (TimeUnitVisitor) myTimeUnitVisitors
                    .get(i);
            if (!nextVisitor.isEnabled()) {
                continue;
            }
            nextVisitor.afterProcessingTimeFrames();
        }
    }

    void fireFrameStarted(TimeFrame timeFrame) {
        for (int i = 0; i < myTimeUnitVisitors.size(); i++) {
            TimeUnitVisitor nextVisitor = (TimeUnitVisitor) myTimeUnitVisitors
                    .get(i);
            if (!nextVisitor.isEnabled()) {
                continue;
            }
            nextVisitor.startTimeFrame(timeFrame);
        }
    }

    void fireFrameFinished(TimeFrame timeFrame) {
        for (int i = 0; i < myTimeUnitVisitors.size(); i++) {
            TimeUnitVisitor nextVisitor = (TimeUnitVisitor) myTimeUnitVisitors
                    .get(i);
            if (!nextVisitor.isEnabled()) {
                continue;
            }
            nextVisitor.endTimeFrame(timeFrame);
        }
    }

    void fireUnitLineStarted(TimeUnit timeUnit) {
        for (int i = 0; i < myTimeUnitVisitors.size(); i++) {
            TimeUnitVisitor nextVisitor = (TimeUnitVisitor) myTimeUnitVisitors
                    .get(i);
            if (!nextVisitor.isEnabled()) {
                continue;
            }
            nextVisitor.startUnitLine(timeUnit);
        }
    }

    void fireUnitLineFinished(TimeUnit timeUnit) {
        for (int i = 0; i < myTimeUnitVisitors.size(); i++) {
            TimeUnitVisitor nextVisitor = (TimeUnitVisitor) myTimeUnitVisitors
                    .get(i);
            if (!nextVisitor.isEnabled()) {
                continue;
            }
            nextVisitor.endUnitLine(timeUnit);
        }
    }

    void visitTimeUnits(TimeFrame timeFrame, TimeUnit timeUnit) {
        for (int j = 0; j < timeFrame.getUnitCount(timeUnit); j++) {
            for (int i = 0; i < myTimeUnitVisitors.size(); i++) {
                TimeUnitVisitor nextVisitor = (TimeUnitVisitor) myTimeUnitVisitors
                        .get(i);
                if (!nextVisitor.isEnabled()) {
                    continue;
                }
                nextVisitor.nextTimeUnit(j);
            }
        }

    }

    public void setBounds(Dimension bounds) {
        myBounds = bounds;
    }

    public void setStartDate(Date startDate) {
        if (!startDate.equals(myStartDate)) {
            myStartDate = startDate;
            myTimeFrames = null;
        }
        myRange2DefaultUnitOffsets.clear();
    }

    public Date getStartDate() {
        return myStartDate;
    }

    public Date getEndDate() {
        TimeFrame[] timeFrames = getTimeFrames(null);
        // for(int i = 0 ; i<timeFrames.length; i++)
        // System.out.println("< "+timeFrames[i].getStartDate() + "" +
        // timeFrames[i].getFinishDate()+" >");

        TimeFrame last = timeFrames[timeFrames.length - 1];
        return last.getFinishDate();
    }

    public void setBottomUnitWidth(int pixelsWidth) {
    	if (myAtomUnitPixels!=pixelsWidth) {
    		myCachingOffsetCalculator.reset();
    	}
        myAtomUnitPixels = pixelsWidth;
    }

    public void setRowHeight(int rowHeight) {
        getChartUIConfiguration().setRowHeight(rowHeight);
    }

    public void setTopTimeUnit(TimeUnit topTimeUnit) {
        setTopUnit(topTimeUnit);
        myTimeFrames = null;
    }

    public void setBottomTimeUnit(TimeUnit bottomTimeUnit) {
        myBottomUnit = bottomTimeUnit;
        myTimeFrames = null;
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
        List/*<Offset>*/ topUnitOffsets = new ArrayList/*<Offset>*/();
        List/*<Offset>*/ bottomUnitOffsets = new ArrayList/*<Offset>*/();
        offsetBuilder.constructOffsets(topUnitOffsets, bottomUnitOffsets);
        int width = ((Offset)topUnitOffsets.get(topUnitOffsets.size()-1)).getOffsetPixels();
        int height = calculateRowHeight()*getRowCount();
        return new Dimension(width, height);
    }
    
    public abstract int calculateRowHeight();
    protected abstract int getRowCount();
    
    TimeFrame[] getTimeFrames(Date exactDate) {
        if (myTimeFrames == null) {
            myTimeFrames = calculateTimeFrames(exactDate);
        }
        return myTimeFrames;
    }

    protected int getBottomUnitWidth() {
        return myAtomUnitPixels;
    }

    private TimeFrame[] calculateTimeFrames(Date exactDate) {
        ArrayList result = new ArrayList();
        int totalFramesWidth = 0;
        Date currentDate = myStartDate;
        do {

            TimeFrame currentFrame = myTimeUnitStack.createTimeFrame(
                    currentDate, getTopUnit(currentDate), myBottomUnit);
            if (exactDate != null
                    && currentFrame.getStartDate().before(exactDate)) {
                currentFrame.trimLeft(exactDate);
            }
            if (currentFrame.getStartDate().after(currentFrame.getFinishDate())) {
                throw new IllegalStateException("Frame is invalid:\n"+currentFrame+"\n date="+exactDate);
            }
            result.add(currentFrame);
            int frameWidth = myFrameWidthFunction
                    .getTimeFrameWidth(currentFrame);
            totalFramesWidth += frameWidth;
            currentDate = currentFrame.getFinishDate();

        } while (totalFramesWidth <= getBounds().getWidth());
        //
        return (TimeFrame[]) result.toArray(new TimeFrame[0]);
    }

    public GPCalendar.DayType getDayType(TimeFrame timeFrame,
            TimeUnit timeUnit, int unitIndex) {
        Date startDate = timeFrame.getUnitStart(timeUnit, unitIndex);
        Date endDate = timeFrame.getUnitFinish(timeUnit, unitIndex);
        Calendar c = CalendarFactory.newCalendar();
        c.setTime(startDate);
        int startDayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        c.setTime(endDate);
        int endDayOfWeek = c.get(Calendar.DAY_OF_WEEK);

        // return startDayOfWeek==Calendar.SATURDAY ||
        // startDayOfWeek==Calendar.SUNDAY;
        // return getTaskManager().getCalendar().getWeekDayType(startDayOfWeek);

        return getTaskManager().getCalendar().getDayTypeDate(startDate);
    }

    public void startDateChanged(ChartViewState.ViewStateEvent e) {
        setStartDate((Date) e.getNewValue());
    }

    public void zoomChanged(ZoomEvent e) {
    }

    /**
     * @author bard
     */
    private interface TimeFrameWidthFunction {
        int getTimeFrameWidth(TimeFrame timeFrame);
    }

    protected TimeUnit getBottomUnit() {
        return myBottomUnit;
    }

    protected TimeUnitStack getTimeUnitStack() {
        return myTimeUnitStack;
    }

    protected DayTypeAlternance[] getDayTypeAlternance(TimeFrame timeFrame,
            TimeUnit timeUnit, int unitIndex) {
        class AlternanceFactory {
            private Calendar c = CalendarFactory.newCalendar();

            DayTypeAlternance createAlternance(TimeUnit timeUnit,
                    Date startDate, Date endDate) {
                c.setTime(startDate);
                int startDayOfWeek = c.get(Calendar.DAY_OF_WEEK);
                c.setTime(endDate);
                int endDayOfWeek = c.get(Calendar.DAY_OF_WEEK);
                TaskLength duration = myTaskManager.createLength(timeUnit,
                        startDate, endDate);
                DayType dayType = getTaskManager().getCalendar()
                        .getWeekDayType(startDayOfWeek);
                dayType = getTaskManager().getCalendar().getDayTypeDate(
                        startDate);
                return new DayTypeAlternance(dayType, duration, endDate);
            }
            void createAlternance(TimeUnit timeUnit, TimeFrame timeFrame, List output) {
                DayType startType = null;
                Date startDate = null;
                int unitCount = timeFrame.getUnitCount(timeUnit);
                for (int i=0; i<unitCount; i++) {
                    Date start = timeFrame.getUnitStart(timeUnit, i);
                    c.setTime(start);
                    //int startDayOfWeek = c.get(Calendar.DAY_OF_WEEK);
                    DayType dayType = getTaskManager().getCalendar().getDayTypeDate(start);
                    if (startType==null) {
                        startType = dayType;
                        startDate = start;
                    }
                    if (startType!=dayType) {
                        Date end = timeFrame.getUnitFinish(timeUnit,i-1);
                        TaskLength duration = myTaskManager.createLength(timeUnit, startDate, end);
                        output.add(new DayTypeAlternance(startType, duration, end));
                        startType = dayType;
                        startDate = start;
                    }
                }
                Date end = timeFrame.getUnitFinish(timeUnit,unitCount-1);
                TaskLength duration = myTaskManager.createLength(timeUnit, startDate, end);
                output.add(new DayTypeAlternance(startType, duration, end));

            }

        }
        AlternanceFactory f = new AlternanceFactory();

        DayTypeAlternance[] result;
        Date startDate = timeFrame.getUnitStart(timeUnit, unitIndex);
        Date endDate = timeFrame.getUnitFinish(timeUnit, unitIndex);

        if (timeUnit.equals(myTimeUnitStack.getDefaultTimeUnit())) {
            result = new DayTypeAlternance[] { f.createAlternance(timeUnit,
                    startDate, endDate) };
        } else if (timeUnit.isConstructedFrom(myTimeUnitStack
                .getDefaultTimeUnit())) {
            java.util.List buf = new ArrayList();
            TimeUnit defaultUnit = myTimeUnitStack.getDefaultTimeUnit();
            TimeFrame innerFrame = myTimeUnitStack.createTimeFrame(startDate,
                    timeUnit, defaultUnit);
            // System.err.println("[ChartModelImpl] topUnit="+timeUnit+"
            // bottom="+defaultUnit+"
            // count="+innerFrame.getUnitCount(defaultUnit));
            f.createAlternance(defaultUnit, innerFrame, buf);
            result = (DayTypeAlternance[]) buf
                    .toArray(new DayTypeAlternance[buf.size()]);
        } else {
            throw new RuntimeException("We should not be here");
        }
        // System.err.println("from "+startDate+" to
        // "+endDate+"\n"+java.util.Arrays.asList(result));
        return result;
    }

    Offset[] calculateOffsets(TimeFrame timeFrame, TimeUnit frameBottomUnit, Date bottomUnitStartDate, TimeUnit offsetUnit, int frameBottomUnitWidth) {
        return myCachingOffsetCalculator.calculateOffsets(timeFrame, frameBottomUnit, bottomUnitStartDate, offsetUnit, frameBottomUnitWidth);
    }

    //private final OffsetCalculatorImpl myOffsetCalculator;
    protected final ChartUIConfiguration myChartUIConfiguration;

    public ChartUIConfiguration getChartUIConfiguration() {
        return myChartUIConfiguration;
    }

    class RegularFramesWithFunction implements TimeFrameWidthFunction {
        public int getTimeFrameWidth(TimeFrame timeFrame) {
            return timeFrame.getUnitCount(myBottomUnit) * myAtomUnitPixels;
        }
    }

    class SkewedFramesWidthFunction implements TimeFrameWidthFunction {
        private float myWidthPerDefaultUnit;

        void initialize() {
            int defaultUnitsPerBottomUnit = myBottomUnit
                    .getAtomCount(myTimeUnitStack.getDefaultTimeUnit());
            myWidthPerDefaultUnit = (float) myAtomUnitPixels
                    / defaultUnitsPerBottomUnit;
        }

        public int getTimeFrameWidth(TimeFrame timeFrame) {
            int defaultUnitsPerTopUnit = timeFrame.getUnitCount(myTimeUnitStack
                    .getDefaultTimeUnit());
            return (int) (defaultUnitsPerTopUnit * myWidthPerDefaultUnit);
        }

    }

    int getBottomUnitWidth(TimeFrame nextFrame) {
        int frameWidth = myFrameWidthFunction.getTimeFrameWidth(nextFrame);
        int bottomUnitsCount = nextFrame
                .getUnitCount(nextFrame.getBottomUnit());
        // System.err.println("ChartModelImpl: getBottomUnitWidth:
        // nextFrame="+nextFrame+" width="+frameWidth+"
        // bottomUnitsCount="+bottomUnitsCount);
        return frameWidth / bottomUnitsCount;
    }

    protected final TaskManager myTaskManager;

    private int myVerticalOffset;

    public TaskManager getTaskManager() {
        return myTaskManager;
    }

    public ChartHeader getChartHeader() {
        return myChartHeader;
    }

    protected ChartGridImpl getChartGrid() {
        return myChartGrid;
    }

    public float calculateLength(int fromX, int toX, int y) {
        // return toX - fromX;

        int curX = fromX;
        int totalPixels = toX - fromX;
        int holidayPixels = 0;
        while (curX < toX) {
            GraphicPrimitiveContainer.GraphicPrimitive nextPrimitive = getChartGrid()
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

    private void setTopUnit(TimeUnit myTopUnit) {
        this.myTopUnit = myTopUnit;
    }

    protected TimeUnit getTopUnit() {
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
        return new GPOptionGroup[] {myChartGrid.getOptions()};
    }

    public void addOptionChangeListener(GPOptionChangeListener listener) {
        myOptionListeners.add(listener);
    }

    protected void fireOptionsChanged() {
        for (int i = 0; i < myOptionListeners.size(); i++) {
            GPOptionChangeListener next = (GPOptionChangeListener) myOptionListeners
                    .get(i);
            next.optionsChanged();
        }
    }

//    public ChartModelBase createCopy() {
//        return new ChartModelBase(getTaskManager(), getTimeUnitStack(), myProjectConfig);
//    }
    public abstract ChartModelBase createCopy();
    protected void setupCopy(ChartModelBase copy) {
        copy.myTopUnit = this.myTopUnit;
        copy.setBottomTimeUnit(getBottomUnit());
        copy.setBottomUnitWidth(getBottomUnitWidth());
        copy.setStartDate(getStartDate());
        copy.setBounds(getBounds());
        copy.getChartUIConfiguration().setRowHeight(calculateRowHeight());
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
        private TaskLength myOffsetLength;
        private Date myOffsetAnchor;
        private Date myOffsetEnd;
        private int myOffsetPixels;
        private TimeUnit myOffsetUnit;
        private GPCalendar.DayType myDayType;

        Offset(TimeUnit offsetUnit, Date offsetAnchor, Date offsetEnd, int offsetPixels, GPCalendar.DayType dayType) {
            myOffsetAnchor = offsetAnchor;
            myOffsetEnd = offsetEnd;
            myOffsetPixels = offsetPixels;
            myOffsetUnit = offsetUnit;
            myDayType = dayType;
        }
//        TaskLength getOffsetLength() {
//            return null;
//        }
        Date getOffsetAnchor() {
            return myOffsetAnchor;
        }
        public Date getOffsetEnd() {
            return myOffsetEnd;
        }
        public int getOffsetPixels() {
            return myOffsetPixels;
        }
        TimeUnit getOffsetUnit() {
            return myOffsetUnit;
        }
        public DayType getDayType() {
            return myDayType;
        }
        public String toString() {
            return "end date: " + myOffsetEnd + " end pixel: " + myOffsetPixels;
        }
        public boolean equals(Object that) {
            if (false==that instanceof Offset) {
                return false;
            }
            Offset thatOffset = (Offset) that;
            return myOffsetPixels==thatOffset.myOffsetPixels &&
                   myOffsetEnd.equals(thatOffset.myOffsetEnd) &&
                   myOffsetAnchor.equals(thatOffset.myOffsetAnchor);
        }
        public int hashCode() {
            return myOffsetEnd.hashCode();
        }


    }
}
