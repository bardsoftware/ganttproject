/***************************************************************************

 GanttGraphicArea.java  -  description

 -------------------

 begin                : dec 2002

 copyright            : (C) 2002 by Thomas Alexandre

 email                : alexthomas(at)ganttproject.org

 ***************************************************************************/

/***************************************************************************

 *                                                                         *

 *   This program is free software; you can redistribute it and/or modify  *

 *   it under the terms of the GNU General Public License as published by  *

 *   the Free Software Foundation; either version 2 of the License, or     *

 *   (at your option) any later version.                                   *

 *                                                                         *

 ***************************************************************************/

package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jdesktop.jdnc.JNTreeTable;
import org.jdesktop.swing.JXTreeTable;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelImpl;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.DependencyInteractionRenderer;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.PublicHolidayDialogAction;
import net.sourceforge.ganttproject.chart.RenderedChartImage;
import net.sourceforge.ganttproject.chart.RenderedGanttChartImage;
import net.sourceforge.ganttproject.chart.TaskInteractionHintRenderer;
import net.sourceforge.ganttproject.chart.VisibleNodesFilter;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueDispatcher;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.GPOptionChangeListener;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomPropertyEvent;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskImpl;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskScheduleAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.task.event.TaskDependencyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.gregorian.GregorianCalendar;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import javax.swing.*;

/**
 * Class for the graphic part of the soft
 */
public class GanttGraphicArea extends ChartComponentBase implements GanttChart,
        CustomPropertyListener, ProjectEventListener {

    static {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        URL cursorResource = GanttGraphicArea.class.getClassLoader()
                .getResource("icons/cursorpercent.gif");
        Image image = toolkit.getImage(cursorResource);
        CHANGE_PROGRESS_CURSOR = toolkit.createCustomCursor(image, new Point(
                10, 5), "CursorPercent");
    }

    private static final Cursor W_RESIZE_CURSOR = new Cursor(
            Cursor.W_RESIZE_CURSOR);

    private static final Cursor E_RESIZE_CURSOR = new Cursor(
            Cursor.E_RESIZE_CURSOR);

    private static final Cursor CHANGE_PROGRESS_CURSOR;

    private static final int HEADER_OFFSET = 47;

    /** Begin of display. */

//    public GanttCalendar date;

    /** Reference to the GanttTree */

    public GanttTree2 tree;

    /** Default color for tasks */

    public static Color taskDefaultColor

    // = new Color( (float) 0.549, (float) 0.713, (float) 0.807);

    = new Color(140, 182, 206);

    GanttProject appli;

    private UIConfiguration myUIConfiguration;

    private Color myProjectLevelTaskColor;

    private final ChartModelImpl myChartModel;

    private final TaskManager myTaskManager;

    private GPUndoManager myUndoManager;

    private JTableHeader myTableHeader = null;


    //private List myItemsToConsider;

    private JPanel myPreviewPanel = new ChartOptionsPreviewPanel();

    private TaskTreeImageGenerator myTaskImageGenerator;

    private ChartViewState myViewState;
    /** Constructor */

    public GanttGraphicArea(GanttProject app, GanttTree2 ttree,
            TaskManager taskManager, ZoomManager zoomManager,
            GPUndoManager undoManager) {
        super(app.getProject(), app.getUIFacade(), zoomManager);
        this.setBackground(Color.WHITE);
        myTaskManager = taskManager;
        myUndoManager = undoManager;
        //
        myChartModel = new ChartModelImpl(getTaskManager(), app
                .getTimeUnitStack(), app.getUIConfiguration());
        myChartModel.addOptionChangeListener(new GPOptionChangeListener() {
            public void optionsChanged() {
                repaint();

            }
        });
        this.tree = ttree;
        myTableHeader = tree.getTreeTable().getTable().getTableHeader();
        myViewState = new ChartViewState(this, app.getUIFacade());
        super.setStartDate(GregorianCalendar.getInstance().getTime());
        myTaskManager.addTaskListener(new TaskListenerAdapter() {
            public void taskScheduleChanged(TaskScheduleEvent e) {
                adjustDependencies((Task) e.getSource());
            }

            public void dependencyAdded(TaskDependencyEvent e) {
                adjustDependencies(e.getDependency().getDependee());
                repaint();
            }

            public void dependencyRemoved(TaskDependencyEvent e) {
                repaint();
            }

            private void adjustDependencies(Task task) {
                RecalculateTaskScheduleAlgorithm alg = myTaskManager
                        .getAlgorithmCollection()
                        .getRecalculateTaskScheduleAlgorithm();
                if (!alg.isRunning()) {
                    try {
                        alg.run(task);
                    } catch (TaskDependencyException e1) {
                        e1.printStackTrace(); // To change body of catch
                        // statement use File | Settings
                        // | File Templates.
                    }
                    // appli.setQuickSave (true);
                }
            }
        });

//        date = new GanttCalendar();
//
//        date.setDay(1);


        appli = app;

        // creation of the different color use to paint

        // arrayColor[0] = new Color((float)0.905,(float)0.905,(float)0.905);

        getProject().getTaskCustomColumnManager().addListener(this);
        myTaskImageGenerator = new TaskTreeImageGenerator(ttree, app.getUIConfiguration());
    }

    /** Return the color of the task */

    public Color getTaskColor() {
        return myUIConfiguration.getTaskColor();
    }

    /** Change the color of the task */

    public void setProjectLevelTaskColor(Color c) {
        myUIConfiguration.setProjectLevelTaskColor(c);
    }

    /** The size of the panel. */

    public Dimension getPreferredSize() {

        return new Dimension(465, 600);

    }

    public ChartModelImpl getMyChartModel() {
        return myChartModel;
    }

    public void drawGPVersion(Graphics g) {
        g.setColor(Color.black);
        g.setFont(Fonts.GP_VERSION_FONT);
        g.drawString("GanttProject (" + GanttProject.version + ")", 3,
                getHeight() + 18);
    }

    public String getName() {
        return GanttLanguage.getInstance().getText("gantt");
    }

    /** Return an image with the gantt chart */
    // TODO: 1.11 take into account flags "render this and don't render that"

    public BufferedImage getChart(GanttExportSettings settings) {
        RenderedChartImage renderedImage = (RenderedChartImage) getRenderedImage(settings);
        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();
        System.err.println("width="+width+" height="+height);
        BufferedImage result = renderedImage.getWholeImage();
        repaint();
        return result;
    }

    public RenderedImage getRenderedImage(GanttExportSettings settings) {

        GPTreeTableBase treetable = Mediator.getGanttProjectSingleton().getTree().getTreeTable();
        JXTreeTable xtreetable = treetable.getTreeTable();

//      I don't know why we need to add 67 to the height to make it fit the real height
        int tree_height = xtreetable.getHeight()+67;

        GanttImagePanel logo_panel= new GanttImagePanel("big.png", 1024, 44);
        BufferedImage tree  = new BufferedImage(xtreetable.getWidth(), tree_height, BufferedImage.TYPE_INT_RGB);
        BufferedImage treeview = new BufferedImage(treetable.getWidth(), treetable.getHeight(), BufferedImage.TYPE_INT_RGB);
        BufferedImage logo  = new BufferedImage(xtreetable.getWidth(), 44, BufferedImage.TYPE_INT_RGB);

        Graphics2D glogo = logo.createGraphics();
        logo_panel.paintComponent(glogo);

        Graphics2D gtreeview = treeview.createGraphics();
        treetable.paintComponents(gtreeview);

        BufferedImage header = treeview.getSubimage(0, 0, treeview.getWidth(), treetable.getRowHeight()+3);
        treeview.flush();

        Graphics2D gtree = tree.createGraphics();
        xtreetable.printAll(gtree);

        //create a new image that will contain the logo, the table/tree and the chart
        BufferedImage task_image = new BufferedImage(xtreetable.getWidth(), tree_height+logo_panel.getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D gimage = task_image.createGraphics();

        //draw the logo on the image
        gimage.drawImage(logo, 0, 0, tree.getWidth(), logo.getHeight(), Color.WHITE, null);
        //draw the header on the image
        gimage.drawImage(header, 0, logo.getHeight(), header.getWidth(), header.getHeight(), null);
        //draw the tree on the image
        gimage.drawImage(tree, 0, logo.getHeight()+header.getHeight(), tree.getWidth(), tree.getHeight(), null);

        Date dateStart = null;
        Date dateEnd = null;

        TimeUnit unit = getViewState().getBottomTimeUnit();

        dateStart = settings.getStartDate() == null ? getStartDate() : settings.getStartDate();
        dateEnd = settings.getEndDate() == null ? getEndDate() : settings.getEndDate();

        if (dateStart.after(dateEnd)) {
            Date tmp = (Date) dateStart.clone();
            dateStart = (Date) dateEnd.clone();
            dateEnd = tmp;
        }

        TaskLength printedLength = getTaskManager().createLength(unit, dateStart, dateEnd);
        System.err.println("start date="+dateStart+" end date="+dateEnd+" unit="+unit+" printed length="+printedLength);
        int chartWidth = (int) ((printedLength.getLength(getViewState().getBottomTimeUnit()) + 1) * getViewState().getBottomUnitWidth());
        if (chartWidth<this.getWidth()) {
            chartWidth = this.getWidth();
        }
        int chartHeight = task_image.getHeight();
        List<DefaultMutableTreeNode> myItemsToConsider = myTaskImageGenerator.getPrintableNodes(settings);

        return new RenderedGanttChartImage(myChartModel, myChartComponentImpl, GanttTree2.convertNodesListToItemList(myItemsToConsider), task_image, chartWidth, chartHeight);
    }

    private GanttTree2 getTree() {

        return this.tree;

    }

    GPUndoManager getUndoManager() {
        return myUndoManager;
    }

    protected ChartModelBase getChartModel() {
        return myChartModel;
    }

    protected MouseListener getMouseListener() {
        return new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                getChartImplementation().getMouseListener().mouseClicked(e);
            }
            public void mouseEntered(MouseEvent e) {
                getChartImplementation().getMouseListener().mouseEntered(e);
            }
            public void mouseExited(MouseEvent e) {
                getChartImplementation().getMouseListener().mouseExited(e);
            }
            public void mousePressed(MouseEvent e) {
                getChartImplementation().getMouseListener().mousePressed(e);
            }
            public void mouseReleased(MouseEvent e) {
                getChartImplementation().getMouseListener().mouseReleased(e);
            }
        };
    }

    protected MouseMotionListener getMouseMotionListener() {
        return new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
                getChartImplementation().getMouseMotionListener().mouseDragged(e);
            }
            public void mouseMoved(MouseEvent e) {
                getChartImplementation().getMouseMotionListener().mouseMoved(e);
            }
        };
    }

    private Action[] getPopupMenuActions() {
        return new Action[] { getOptionsDialogAction(),
                new PublicHolidayDialogAction(getProject(), getUIFacade()) };
        // actions.add(createMenuAction(GanttProject.correctLabel(language
        // .getText("editPublicHolidays")), "));
    }


    protected Component createPreviewComponent() {
        return myPreviewPanel;
    }

    public void repaint() {
        try {
            if (myChartModel!=null) {
                myChartModel.setHeaderHeight(myTableHeader.getHeight()
                        + HEADER_OFFSET);
            }
        } catch (NullPointerException e) {
            if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
            }
        }
        super.repaint();
    }

    class MouseSupport {
        protected Task findTaskUnderMousePointer(int xpos, int ypos) {
            // int taskID = detectPosition(xpos, ypos, false);
            // return taskID==-1 ? null : getTaskManager().getTask(taskID);
            ChartItem chartItem = myChartModel.getChartItemWithCoordinates(
                    xpos, ypos);
            return chartItem == null ? null : chartItem.getTask();
        }

        protected ChartItem getChartItemUnderMousePoint(int xpos, int ypos) {
            ChartItem result = myChartModel.getChartItemWithCoordinates(xpos,
                    ypos);
            return result;
        }
    }

    abstract class ChangeTaskBoundaryInteraction extends MouseInteractionBase {
        private TaskInteractionHintRenderer myLastNotes;

        private final Task myTask;

        private final float myInitialDuration;

        private GanttCalendar myInitialEnd;

        private GanttCalendar myInitialStart;

        protected ChangeTaskBoundaryInteraction(MouseEvent initiatingEvent,
                TaskBoundaryChartItem taskBoundary) {
            super(initiatingEvent);
            myTask = taskBoundary.getTask();
            myInitialDuration = myTask.getDuration().getLength(
                    getViewState().getBottomTimeUnit());
            myInitialEnd = getTask().getEnd();
            myInitialStart = getTask().getStart();
        }

        public void apply(MouseEvent e) {
            if (myLastNotes == null) {
                myLastNotes = new TaskInteractionHintRenderer("", e.getX(), e
                        .getY());
            }
            float diff = getLengthDiff(e);
            apply(diff);
            myLastNotes.setString(getNotesText());
            myLastNotes.setX(e.getX());
        }

        protected Task getTask() {
            return myTask;
        }

        protected float getInitialDuration() {
            return myInitialDuration;
        }

        public void finish(final TaskMutator mutator) {
            mutator.setIsolationLevel(TaskMutator.READ_UNCOMMITED);

            // if
            // ((!myInitialEnd.equals(getTask().getEnd()))||(!myInitialStart.equals(getTask().getStart())))
            getUndoManager().undoableEdit("Task boundary changed",
                    new Runnable() {
                        public void run() {
                            doFinish(mutator);
                        }
                    });

        }

        private void doFinish(TaskMutator mutator) {
            mutator.commit();
            myLastNotes = null;
            try {
                getTaskManager().getAlgorithmCollection()
                        .getRecalculateTaskScheduleAlgorithm().run();
            } catch (TaskDependencyException e) {
                if (!GPLogger.log(e)) {
                    e.printStackTrace(System.err);
                }
                getUIFacade().showErrorDialog(e);
            }
            GanttGraphicArea.this.repaint();
        }

        public void paint(Graphics g) {
            if (myLastNotes != null) {
                myLastNotes.paint(g);
            }
        }

        protected abstract void apply(float diff);

        protected abstract String getNotesText();
    }

    class ChangeTaskEndInteraction extends ChangeTaskBoundaryInteraction
            implements MouseInteraction {
        private TaskMutator myMutator;

        private GanttCalendar myInitialEnd;

        public ChangeTaskEndInteraction(MouseEvent initiatingEvent,
                TaskBoundaryChartItem taskBoundary) {
            super(initiatingEvent, taskBoundary);
            setCursor(E_RESIZE_CURSOR);
            myMutator = getTask().createMutator();
            myInitialEnd = getTask().getEnd();
        }

        protected void apply(float diff) {
            TaskLength newLength = getTaskManager().createLength(
                    getViewState().getBottomTimeUnit(),
                    getInitialDuration() + diff);
            TaskLength translated = getTask().translateDuration(newLength);
            if (translated.getLength() != 0) {
                myMutator.setDuration(translated);
            }
        }

        protected String getNotesText() {
            return getTask().getEnd().toString();
        }

        public void finish() {
            super.finish(myMutator);
        }
    }

    class ChangeTaskStartInteraction extends ChangeTaskBoundaryInteraction
            implements MouseInteraction {
        private TaskLength myInitialLength;

        private TaskMutator myMutator;

        private GanttCalendar myInitialStart;

        ChangeTaskStartInteraction(MouseEvent e,
                TaskBoundaryChartItem taskBoundary) {
            super(e, taskBoundary);
            setCursor(W_RESIZE_CURSOR);
            myInitialLength = getTask().getDuration();
            myMutator = getTask().createMutator();
            myInitialStart = getTask().getStart();
        }

        protected void apply(float diff) {
            TaskLength bottomUnitDiff = getTaskManager().createLength(
                    getViewState().getBottomTimeUnit(), diff);
            TaskLength taskUnitDiff = getTask().translateDuration(bottomUnitDiff);
            if (taskUnitDiff.getValue() != 0) {
                Date newStart = getTaskManager().shift(myInitialStart.getTime(), taskUnitDiff);
                myMutator.setStart(new GanttCalendar(newStart));
                if ((getTask().getThird() != null)
                        && (getTask().getThirdDateConstraint() == TaskImpl.EARLIESTBEGIN))
                    myMutator.setEnd(getTask().getEnd().Clone());
                getTask().applyThirdDateConstraint();
            }
        }

        public void finish() {
            super.finish(myMutator);
            getTask().applyThirdDateConstraint();
        }

        protected String getNotesText() {
            return getTask().getStart().toString();
        }
    }

    class ChangeTaskProgressInteraction extends MouseInteractionBase implements
            MouseInteraction {
        private TaskProgressChartItem myTaskProgrssItem;

        private TaskMutator myMutator;

        private TaskInteractionHintRenderer myLastNotes;

        private int myProgressWas;

        private int myProgressIs;

        public ChangeTaskProgressInteraction(MouseEvent e,
                TaskProgressChartItem taskProgress) {
            super(e);
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            try {
                setCursor(CHANGE_PROGRESS_CURSOR);
            } catch (Exception exept) {
                setCursor(E_RESIZE_CURSOR);
            }
            myTaskProgrssItem = taskProgress;
            myMutator = myTaskProgrssItem.getTask().createMutator();
            myProgressWas = myTaskProgrssItem.getTask()
                    .getCompletionPercentage();
        }

        public void apply(MouseEvent event) {
            // int deltaProgress =
            // (int)myTaskProgrssItem.getProgressDelta(event.getX());
            float deltaUnits = getLengthDiff(event);
            int deltaPercents = (int) (100 * deltaUnits / myTaskProgrssItem
                    .getTask().getDuration().getLength(
                            getViewState().getBottomTimeUnit()));
            int newProgress = myProgressWas + deltaPercents;
            if (newProgress > 100) {
                newProgress = 100;
            }
            if (newProgress < 0) {
                newProgress = 0;
            }
            myProgressIs = newProgress;
            myMutator.setCompletionPercentage(newProgress);
            myLastNotes = new TaskInteractionHintRenderer(newProgress + "%",
                    event.getX(), event.getY() - 30);
        }

        public void finish() {

            myMutator.setIsolationLevel(TaskMutator.READ_COMMITED);

            getUndoManager().undoableEdit("Task progress changed",
                    new Runnable() {
                        public void run() {
                            doFinish(myMutator);
                        }
                    });
            GanttGraphicArea.this.repaint();
        }

        private void doFinish(TaskMutator mutator) {
            mutator.commit();
            myLastNotes = null;
            try {
                getTaskManager().getAlgorithmCollection()
                        .getRecalculateTaskScheduleAlgorithm().run();
            } catch (TaskDependencyException e) {
                getUIFacade().showErrorDialog(e);
            }
            if (myProgressIs == myProgressWas) {
                // getUndoManager ().die ();

                myMutator.commit();
                repaint();
                int myProgressIs = myTaskProgrssItem.getTask()
                        .getCompletionPercentage();
                if (myProgressIs != myProgressWas) {
                    // appli.setQuickSave(true);
                    // appli.quickSave ("Task progress changed");

                }

            }
        }

        public void paint(Graphics g) {
            if (myLastNotes != null) {
                myLastNotes.paint(g);
            }
        }
    }

    class DrawDependencyInteraction extends MouseInteractionBase implements
            MouseInteraction {

        private final Task myTask;

        private Point myStartPoint;

        private DependencyInteractionRenderer myArrow;

        private GanttGraphicArea.MouseSupport myMouseSupport;

        private Task myDependant;

        private MouseEvent myLastMouseEvent = null;

        public DrawDependencyInteraction(MouseEvent initiatingEvent,
                TaskRegularAreaChartItem taskArea, MouseSupport mouseSupport) {
            super(initiatingEvent);
            myStartPoint = initiatingEvent.getPoint();
            myTask = taskArea.getTask();
            myArrow = new DependencyInteractionRenderer(myStartPoint.x,
                    myStartPoint.y, myStartPoint.x, myStartPoint.y);
            myMouseSupport = mouseSupport;
        }

        public void apply(MouseEvent event) {
            myArrow.changePoint2(event.getX(), event.getY());
            // myDependant = myMouseSupport.findTaskUnderMousePointer(
            // event.getX(), event.getY());
            myLastMouseEvent = event;
        }

        public void finish() {
            if (myLastMouseEvent != null) {
                myDependant = myMouseSupport.findTaskUnderMousePointer(
                        myLastMouseEvent.getX(), myLastMouseEvent.getY());
                final Task dependee = myTask;
                if (myDependant != null) {
                    if (getTaskManager().getDependencyCollection()
                            .canCreateDependency(myDependant, dependee)) {
                        getUndoManager().undoableEdit("Draw dependency",
                                new Runnable() {
                                    public void run() {
                                        try {
                                            getTaskManager()
                                                    .getDependencyCollection()
                                                    .createDependency(
                                                            myDependant,
                                                            dependee,
                                                            new FinishStartConstraintImpl());

                                        } catch (TaskDependencyException e) {
                                            getUIFacade().showErrorDialog(e);
                                        }
                                        appli.setAskForSave(true);
                                        // appli.setQuickSave (true);
                                        // appli.quickSave ("Draw dependency");
                                    }
                                });
                    }
                } else {
                    myArrow = new DependencyInteractionRenderer();
                    repaint();
                }
            }

        }

        public void paint(Graphics g) {
            myArrow.paint(g);
        }
    }

    class MoveTaskInteraction extends MouseInteractionBase implements
            MouseInteraction {
        private Task myTask;

        private TaskMutator myMutator;

        private GanttCalendar myInitialStart;

        MoveTaskInteraction(MouseEvent e, Task task) {
            super(e);
            myTask = task;
            myMutator = task.createMutator();
            myInitialStart = myTask.getStart();
        }

        public void apply(MouseEvent event) {
            float diff = getChartModel().calculateLengthNoWeekends(getStartX(),
                    event.getX());
            TaskLength bottomUnitLength = getTaskManager().createLength(
                    getViewState().getBottomTimeUnit(), diff);
            TaskLength taskLength = myTask.translateDuration(bottomUnitLength);
            int dayDiff = (int) (taskLength.getValue());
            // System.err.println("[MoveTaskInteraction] apply():
            // dayDiff="+dayDiff+" bottomUnitLength="+bottomUnitLength+"
            // translated="+taskLength);
            if (dayDiff != 0) {
                myMutator.shift(dayDiff);
            }
        }

        public void finish() {
            myMutator.setIsolationLevel(TaskMutator.READ_COMMITED);
            getUndoManager().undoableEdit("Task moved", new Runnable() {
                public void run() {
                    doFinish();
                }
            });

        }

        private void doFinish() {
            myMutator.commit();
            try {
                getTaskManager().getAlgorithmCollection()
                        .getRecalculateTaskScheduleAlgorithm().run();
            } catch (TaskDependencyException e) {
                getUIFacade().showErrorDialog(e);
            }
            GanttGraphicArea.this.repaint();
        }

    }

    class MoveTaskInteractions extends MouseInteractionBase implements
            MouseInteraction {

        private List<Task> myTasks; // of Task

        private List<TaskMutator> myMutators; // of TaskMutator

        private List<GanttCalendar> myInitialStarts; // of GanttCalendar

        MoveTaskInteractions(MouseEvent e, List<Task> tasks) {
            super(e);
            myTasks = tasks;
            myMutators = new ArrayList<TaskMutator>(myTasks.size());
            myInitialStarts = new ArrayList<GanttCalendar>(myTasks.size());
            Iterator<Task> itTasks = myTasks.iterator();
            while (itTasks.hasNext()) {
                Task t = itTasks.next();
                myMutators.add(t.createMutator());
                myInitialStarts.add(t.getStart());
            }
        }

        public void apply(MouseEvent event) {
            float diff = getLengthDiff(event);
            TaskLength bottomUnitLength = getTaskManager().createLength(
                    getViewState().getBottomTimeUnit(), diff);

            for (int i = 0; i < myTasks.size(); i++) {
                Task task = myTasks.get(i);
                TaskLength taskLength = task
                        .translateDuration(bottomUnitLength);
                int dayDiff = (int) (taskLength.getValue());
                if (dayDiff != 0) {
                    myMutators.get(i).shift(dayDiff);
                }
            }
        }

        public void finish() {
            Iterator<TaskMutator> itMutators = myMutators.iterator();
            while (itMutators.hasNext())
                itMutators.next()
                        .setIsolationLevel(TaskMutator.READ_COMMITED);

            getUndoManager().undoableEdit("Task moved", new Runnable() {
                public void run() {
                    doFinish();
                }
            });

        }

        private void doFinish() {
            Iterator<TaskMutator> itMutators = myMutators.iterator();
            while (itMutators.hasNext())
                itMutators.next().commit();

            try {
                getTaskManager().getAlgorithmCollection()
                        .getRecalculateTaskScheduleAlgorithm().run();
            } catch (TaskDependencyException e) {
                getUIFacade().showErrorDialog(e);
            }

            Iterator<Task> itTasks = myTasks.iterator();
            while (itTasks.hasNext()) {
                Task t = itTasks.next();
                t.applyThirdDateConstraint();
            }

            GanttGraphicArea.this.repaint();
        }
    }

    public interface ChartImplementation extends ZoomListener {
        void paintChart(Graphics g);

        void paintComponent(Graphics g, List/*<Task>*/ visibleTasks);

        MouseListener getMouseListener();

        MouseMotionListener getMouseMotionListener();

        void beginChangeTaskEndInteraction(MouseEvent initiatingEvent,
                TaskBoundaryChartItem taskBoundary);

        MouseInteraction getActiveInteraction();

        void beginChangeTaskStartInteraction(MouseEvent e,
                TaskBoundaryChartItem taskBoundary);

        MouseInteraction finishInteraction();

        void beginChangeTaskProgressInteraction(MouseEvent e,
                TaskProgressChartItem item);

        void beginDrawDependencyInteraction(MouseEvent initiatingEvent,
                TaskRegularAreaChartItem taskArea,
                GanttGraphicArea.MouseSupport mouseSupport);

        void beginMoveTaskInteraction(MouseEvent e, Task task);

        void beginMoveTaskInteractions(MouseEvent e, List<Task> tasks);

        void beginScrollViewInteraction(MouseEvent e);

    }

    private class NewChartComponentImpl extends AbstractChartImplementation implements ChartImplementation {
        public NewChartComponentImpl(IGanttProject project, ChartModelBase chartModel, ChartComponentBase chartComponent) {
            super(project, chartModel, chartComponent);
            // TODO Auto-generated constructor stub
        }

        public void beginChangeTaskEndInteraction(MouseEvent initiatingEvent,
                TaskBoundaryChartItem taskBoundary) {
            setActiveInteraction(new ChangeTaskEndInteraction(initiatingEvent,
                    taskBoundary));
        }

        public void beginChangeTaskStartInteraction(MouseEvent e,
                TaskBoundaryChartItem taskBoundary) {
            setActiveInteraction(new ChangeTaskStartInteraction(e, taskBoundary));
        }

        public void beginChangeTaskProgressInteraction(MouseEvent e,
                TaskProgressChartItem taskProgress) {
            setActiveInteraction(new ChangeTaskProgressInteraction(e,
                    taskProgress));
        }

        public void beginDrawDependencyInteraction(MouseEvent initiatingEvent,
                TaskRegularAreaChartItem taskArea,
                GanttGraphicArea.MouseSupport mouseSupport) {
            setActiveInteraction(new DrawDependencyInteraction(initiatingEvent,
                    taskArea, mouseSupport));
        }

        public void beginMoveTaskInteraction(MouseEvent e, Task task) {
            setActiveInteraction(new MoveTaskInteraction(e, task));
        }

        public void beginMoveTaskInteractions(MouseEvent e, List<Task> tasks) {
            setActiveInteraction(new MoveTaskInteractions(e, tasks));
        }

        public void paintComponent(Graphics g, List/*<Task>*/ visibleTasks) {
            synchronized(ChartModelBase.STATIC_MUTEX) {
                GanttGraphicArea.super.paintComponent(g);
                ChartModel model = myChartModel;
                model.setTaskContainment(appli.getTaskContainment());
                // model.setBounds(getSize());
                // System.err.println("[NewChartComponentImpl] paintComponent. unit
                // width="+getViewState().getBottomUnitWidth());
                model.setBottomUnitWidth(getViewState().getBottomUnitWidth());
                model.setRowHeight(((GanttTree2) tree).getTreeTable()
                        .getRowHeight());
                model.setTopTimeUnit(getViewState().getTopTimeUnit());
                model.setBottomTimeUnit(getViewState().getBottomTimeUnit());
                model.setVisibleTasks(visibleTasks);
                model.paint(g);
                if (getActiveInteraction() != null) {
                    getActiveInteraction().paint(g);
                }
            }
        }

        public void paintChart(Graphics g) {
            synchronized(ChartModelBase.STATIC_MUTEX) {
                //GanttGraphicArea.super.paintComponent(g);
                ChartModel model = myChartModel;
                model.setTaskContainment(appli.getTaskContainment());
                // model.setBounds(getSize());
                // System.err.println("[NewChartComponentImpl] paintComponent. unit
                // width="+getViewState().getBottomUnitWidth());
                model.setBottomUnitWidth(getViewState().getBottomUnitWidth());
                model.setRowHeight(((GanttTree2) tree).getTreeTable()
                        .getRowHeight());
                model.setTopTimeUnit(getViewState().getTopTimeUnit());
                model.setBottomTimeUnit(getViewState().getBottomTimeUnit());
                VisibleNodesFilter visibleNodesFilter = new VisibleNodesFilter();
                List visibleTasks = tree.getVisibleNodes(visibleNodesFilter);
                model.setVisibleTasks(visibleTasks);
                model.paint(g);
                if (getActiveInteraction() != null) {
                    getActiveInteraction().paint(g);
                }
            }
        }

        public MouseListener getMouseListener() {
            return myMouseListener;
        }

        public MouseMotionListener getMouseMotionListener() {
            return myMouseMotionListener;
        }

        public IStatus canPaste(ChartSelection selection) {
            return Status.OK_STATUS;
        }

        public ChartSelection getSelection() {
            ChartSelectionImpl result = new ChartSelectionImpl() {
                public boolean isEmpty() {
                    return false;
                }
                public void startCopyClipboardTransaction() {
                    super.startCopyClipboardTransaction();
                    tree.copySelectedNode();
                }
                public void startMoveClipboardTransaction() {
                    super.startMoveClipboardTransaction();
                    tree.cutSelectedNode();
                }
            };
            return result;
        }

        public void paste(ChartSelection selection) {
            tree.pasteNode();
        }


        private OldChartMouseListenerImpl myMouseListener = new OldChartMouseListenerImpl();

        private OldMouseMotionListenerImpl myMouseMotionListener = new OldMouseMotionListenerImpl();

    }

    protected AbstractChartImplementation getImplementation() {
        return (AbstractChartImplementation) getChartImplementation();
    }

    private ChartImplementation getChartImplementation() {
        if (myChartComponentImpl == null) {
            myChartComponentImpl = new NewChartComponentImpl(getProject(), getChartModel(), this);
        }
        return myChartComponentImpl;
    }

    public Action getScrollCenterAction(ScrollingManager scrollMgr,
            TaskSelectionManager taskSelMgr, String iconSize) {
        if (myScrollCenterAction == null)
            myScrollCenterAction = new ScrollGanttChartCenterAction(scrollMgr,
                    taskSelMgr, iconSize);
        return myScrollCenterAction;
    }

    public void setPreviousStateTasks(ArrayList<GanttPreviousStateTask> tasks) {
        int rowHeight = myChartModel.setPreviousStateTasks(tasks);
        ((GanttTree2) appli.getTree()).getTable().setRowHeight(rowHeight);
    }


    private ChartImplementation myChartComponentImpl;

    private ScrollGanttChartCenterAction myScrollCenterAction;

    protected class ScrollGanttChartCenterAction extends GPAction {
        private final ScrollingManager myScrollingManager;

        private final TaskSelectionManager myTaskSelectionManager;

        public ScrollGanttChartCenterAction(ScrollingManager scrollingManager,
                TaskSelectionManager taskSelectionManager, String iconSize) {
            super("ScrollCenter", iconSize);
            myScrollingManager = scrollingManager;
            myTaskSelectionManager = taskSelectionManager;
        }

        public void actionPerformed(ActionEvent e) {
            getUIFacade().setStatusText(GanttLanguage.getInstance().getText("centerOnSelectedTasks"));
            scroll();
        }

        private void scroll() {
            GanttCalendar min = null;
            GanttCalendar max = null;
            Date scrollDate = null;

            Iterator<Task> it = null;
            if (myTaskSelectionManager.getSelectedTasks().isEmpty()) {
                // scrollDate = getTaskManager().getProjectStart();
                it = Arrays.asList(getTaskManager().getTasks()).iterator();
            } else {
                it = myTaskSelectionManager.getSelectedTasks().iterator();
            }
            while (it.hasNext()) {
                Task t = it.next();
                GanttCalendar dStart = t.getStart();
                GanttCalendar dEnd = t.getEnd();

                min = min == null ? dStart.Clone()
                        : (min.compareTo(dStart) > 0 ? dStart.Clone() : min);
                max = max == null ? dEnd.Clone()
                        : (max.compareTo(dEnd) < 0 ? dEnd.Clone() : max);
            }

            //no tasks defined, nothing to do
            if(min == null || max == null)
                return;

            TimeUnit defaultUnit = getTimeUnitStack().getDefaultTimeUnit();
            final TaskLength selectionLength = getTaskManager().createLength(
                    defaultUnit, min.getTime(), max.getTime());
            final TaskLength viewLength = getChartModel().getVisibleLength();
            float viewLengthInDefaultUnits = viewLength.getLength(defaultUnit);
            // if selection is shorter than view we'll scroll right,
            // otherwise we'll scroll left
            // delta is measured in the bottom line time units
            final float delta = (selectionLength.getValue() - viewLengthInDefaultUnits) / 2;
            scrollDate = GPCalendar.PLAIN.shiftDate(min.getTime(),
                    getTaskManager().createLength(defaultUnit, delta));

            myScrollingManager.scrollLeft(scrollDate);
        }

        /*
         * (non-Javadoc)
         *
         * @see net.sourceforge.ganttproject.action.GPAction#getIconFilePrefix()
         */
        protected String getIconFilePrefix() {
            return "scrollcenter_";
        }

        protected String getLocalizedName() {
            return super.getLocalizedName();
        }
    }

    private class OldChartMouseListenerImpl extends MouseListenerBase implements
            MouseListener {
        private MouseSupport myMouseSupport = new MouseSupport();

        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Task taskUnderPointer = myMouseSupport
                        .findTaskUnderMousePointer(e.getX(), e.getY());
                if (taskUnderPointer == null) {
                    tree.selectTreeRow(-1);
                }
            }
            if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                if (!appli.isOnlyViewer)
                    appli.propertiesTask();
            }
        }

        protected Action[] getPopupMenuActions() {
            Action[] treeActions = tree.getPopupMenuActions();
            int sep = 0;
            if (treeActions.length != 0) {
                sep = 1;
            }
            Action[] chartActions = GanttGraphicArea.this.getPopupMenuActions();
            Action[] result = new Action[treeActions.length + sep
                    + chartActions.length];
            System.arraycopy(treeActions, 0, result, 0, treeActions.length);
            System.arraycopy(chartActions, 0, result, treeActions.length
                    + sep, chartActions.length);
            return result;
        }

        protected void processLeftButton(MouseEvent e) {
            boolean isMineEvent = true;
            ChartItem itemUnderPoint = myMouseSupport.getChartItemUnderMousePoint(e.getX(), e.getY());
            if (itemUnderPoint instanceof TaskBoundaryChartItem) {
                TaskBoundaryChartItem taskBoundary = (TaskBoundaryChartItem) itemUnderPoint;
                if (taskBoundary.isStartBoundary()) {
                    getChartImplementation().beginChangeTaskStartInteraction(e, taskBoundary);
                }
                else {
                    getChartImplementation().beginChangeTaskEndInteraction(e, taskBoundary);
                }
            }
            else if (itemUnderPoint instanceof TaskProgressChartItem) {
                getChartImplementation().beginChangeTaskProgressInteraction(
                        e, (TaskProgressChartItem) itemUnderPoint);
            }
            else if (itemUnderPoint instanceof TaskRegularAreaChartItem) {
                getChartImplementation().beginDrawDependencyInteraction(
                        e, (TaskRegularAreaChartItem) itemUnderPoint, myMouseSupport);
            }
            else {
                isMineEvent = false;
                super.processLeftButton(e);
            }
            if (isMineEvent) {
                repaint();
                appli.recalculateCriticalPath();
            }
        }

        public void mousePressed(MouseEvent e) {
            tree.stopEditing();
            if (appli.isOnlyViewer)
                return;
            Task taskUnderPointer = myMouseSupport.findTaskUnderMousePointer(e
                    .getX(), e.getY());
            if (taskUnderPointer!=null && !Mediator.getTaskSelectionManager().isTaskSelected(
                    taskUnderPointer)) {
                boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
                tree.selectTask(taskUnderPointer, ctrl);
            }
            super.mousePressed(e);
            if (taskUnderPointer == null) {
                return;
            }
            if (e.getButton() == MouseEvent.BUTTON2) {
                if (!Mediator.getTaskSelectionManager().isTaskSelected(
                        taskUnderPointer))
                    tree.selectTask(taskUnderPointer, false);
                List<Task> l = Mediator.getTaskSelectionManager().getSelectedTasks();
                getChartImplementation().beginMoveTaskInteractions(e, l);
            }
        }

    }

    private class OldMouseMotionListenerImpl extends MouseMotionListenerBase {
        private MouseSupport myMouseSupport = new MouseSupport();

        public void mouseDragged(MouseEvent e) {
            if (appli.isOnlyViewer)
                return;
            super.mouseDragged(e);
            /*
             * Add the repaint in order to repaint the treetable when an action
             * occurs on the GraphicArea. here mousedragged because all actions
             * modifying task properties on the graphics are made through
             * mousedragged (I think !)
             */
            // Mediator.getGanttProjectSingleton().repaint();
            // getUIFacade().repaint2();
            if (myUIConfiguration.isCriticalPathOn()) {
                MouseInteraction mi = myChartComponentImpl
                        .getActiveInteraction();
                if ((mi instanceof ChangeTaskBoundaryInteraction)
                        || (mi instanceof MoveTaskInteraction)
                        || (mi instanceof MoveTaskInteractions))
                    appli.recalculateCriticalPath();
            }
            GanttGraphicArea.this.repaint();
            // avant

        }

        // Move the move on the area
        public void mouseMoved(MouseEvent e) {
            ChartItem itemUnderPoint = myMouseSupport
                    .getChartItemUnderMousePoint(e.getX(), e.getY());
            Task taskUnderPoint = itemUnderPoint == null ? null
                    : itemUnderPoint.getTask();
            // System.err.println("[OldMouseMotionListenerImpl] mouseMoved:
            // taskUnderPoint="+taskUnderPoint);
            if (taskUnderPoint == null) {
                setDefaultCursor();
            } else {
                if (itemUnderPoint instanceof TaskBoundaryChartItem) {
                    Cursor cursor = ((TaskBoundaryChartItem) itemUnderPoint)
                            .isStartBoundary() ? W_RESIZE_CURSOR
                            : E_RESIZE_CURSOR;
                    setCursor(cursor);
                }
                // special cursor
                else if (itemUnderPoint instanceof TaskProgressChartItem) {
                    setCursor(CHANGE_PROGRESS_CURSOR);
                } else {
                    setDefaultCursor();
                }
                // getUIFacade().repaint2();
                appli.repaint();
            }
        }
    }

    public void setTaskManager(TaskManager taskManager) {
        // TODO Auto-generated method stub

    }

    public void reset() {
    	repaint();
    }

    public Icon getIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    public void customPropertyChange(CustomPropertyEvent event) {
        repaint();
    }

    public void setUIConfiguration(UIConfiguration configuration) {
        myUIConfiguration = configuration;
    }

    private static class ChartOptionsPreviewPanel extends JPanel implements
            ChangeValueListener {
        Text upText, downText, leftText, rightText;

        TaskBar taskBar;

        public ChartOptionsPreviewPanel() {
            super();
            addToDispatchers();
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(450, 70));

            taskBar = new TaskBar();

            upText = new Text(Text.UP, taskBar);
            downText = new Text(Text.DOWN, taskBar);
            leftText = new Text(Text.LEFT, taskBar);
            rightText = new Text(Text.RIGHT, taskBar);
        }

        void refresh() {

        }
        private void addToDispatchers() {
            List<ChangeValueDispatcher> dispatchers = Mediator.getChangeValueDispatchers();
            for (int i = 0; i < dispatchers.size(); i++) {
                dispatchers.get(i)
                        .addChangeValueListener(this);
            }
        }

        public void paint(Graphics g) {
            super.paint(g);
            taskBar.paintMe(g);
            upText.paintMe(g);
            downText.paintMe(g);
            leftText.paintMe(g);
            rightText.paintMe(g);
        }

        public void changeValue(ChangeValueEvent event) {
            Object id = event.getID();
            if (id.equals("up")) {
                upText.text = getI18n(event.getNewValue().toString());
            } else if (id.equals("down")) {
                downText.text = getI18n(event.getNewValue().toString());
            } else if (id.equals("left")) {
                leftText.text = getI18n(event.getNewValue().toString());
            } else if (id.equals("right")) {
                rightText.text = getI18n(event.getNewValue().toString());
            }
            repaint();
        }

        static String getI18n(String id) {
            String res = GanttLanguage.getInstance().getText(
                    "optionValue." + id + ".label");
            if (res.startsWith(GanttLanguage.MISSING_RESOURCE)) {
                res = id;
            }
            return res;
        }

        class TaskBar {
            int width, height, x, y;

            Color color;

            TaskBar() {
                width = 100;
                height = 12;
                x = (int) (ChartOptionsPreviewPanel.this.getPreferredSize()
                        .getWidth() / 2 - width / 2);
                y = (int) (ChartOptionsPreviewPanel.this.getPreferredSize()
                        .getHeight() / 2 - height / 2);
                color = new Color(140, 182, 206);
            }

            void paintMe(Graphics g) {
                g.setColor(color);
                g.fillRect(x, y, width, height);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, width, height);
            }

        }

        private static class Text {
            static final Font FONT = Fonts.PREVIEW_BAR_FONT;

            static final int LEFT = 0;

            static final int RIGHT = 1;

            static final int UP = 2;

            static final int DOWN = 3;

            static final int MARGIN = 3;

            String text = "";

            int position;

            private int x, y;

            TaskBar taskBar;

            Text(int position, TaskBar refBar) {
                this.position = position;
                this.taskBar = refBar;
            }

            void paintMe(Graphics g) {
                calculateCoordinates(g);
                g.setFont(FONT);
                g.drawString(text, x, y);
            }

            private void calculateCoordinates(Graphics g) {

                int textHeight = g.getFontMetrics(FONT).getHeight();
                int textWidth = g.getFontMetrics(FONT).stringWidth(text);

                switch (position) {
                case UP:
                    y = taskBar.y - MARGIN;
                    x = taskBar.x + taskBar.width / 2 - textWidth / 2;
                    break;
                case DOWN:
                    x = taskBar.x + taskBar.width / 2 - textWidth / 2;
                    y = taskBar.y + taskBar.height + textHeight - MARGIN;
                    break;
                case LEFT:
                    y = taskBar.y + taskBar.height / 2 + textHeight / 2
                            - MARGIN;
                    x = taskBar.x - MARGIN - textWidth;
                    break;
                case RIGHT:
                    y = taskBar.y + taskBar.height / 2 + textHeight / 2
                            - MARGIN;
                    x = taskBar.x + taskBar.width + MARGIN;
                    break;
                }
            }

        }
    }

    public void editTaskAsNew(Task task) {
        if (appli.getOptions().getAutomatic()) {
            appli.propertiesTask();
        }
        else {
        // setQuickSave(true);
            tree.setEditingTask(task);
        }
    }
    public void projectModified() {
        // TODO Auto-generated method stub

    }

    public void projectSaved() {
        // TODO Auto-generated method stub

    }

    public void projectClosed() {
        repaint();
        setProjectLevelTaskColor(null);
        setPreviousStateTasks(null);
    }

    public void projectWillBeOpened() {
    }
    public void projectOpened() {
    }
    @Override
    public ChartViewState getViewState() {
        return myViewState;
    }
}
