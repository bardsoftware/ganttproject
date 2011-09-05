package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelImpl;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.PublicHolidayDialogAction;
import net.sourceforge.ganttproject.chart.VisibleNodesFilter;
import net.sourceforge.ganttproject.chart.export.RenderedChartImage;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.chart.mouse.ChangeTaskEndInteraction;
import net.sourceforge.ganttproject.chart.mouse.ChangeTaskProgressInteraction;
import net.sourceforge.ganttproject.chart.mouse.ChangeTaskStartInteraction;
import net.sourceforge.ganttproject.chart.mouse.DrawDependencyInteraction;
import net.sourceforge.ganttproject.chart.mouse.MouseInteraction;
import net.sourceforge.ganttproject.chart.mouse.MoveTaskInteractions;
import net.sourceforge.ganttproject.chart.mouse.TimelineFacadeImpl;
import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.GPOptionChangeListener;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomPropertyEvent;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskScheduleAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.event.TaskDependencyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.gregorian.GregorianCalendar;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

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

    public GanttTree2 tree;

    public static Color taskDefaultColor = new Color(140, 182, 206);

    private GanttProject appli;

    private UIConfiguration myUIConfiguration;

    private final ChartModelImpl myChartModel;

    private final TaskManager myTaskManager;

    private GPUndoManager myUndoManager;

    private ChartViewState myViewState;

    private GanttPreviousState myBaseline;

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
        myViewState = new ChartViewState(this, app.getUIFacade());
        super.setStartDate(GregorianCalendar.getInstance().getTime());
        myTaskManager.addTaskListener(new TaskListenerAdapter() {
            @Override
            public void taskScheduleChanged(TaskScheduleEvent e) {
                adjustDependencies((Task) e.getSource());
            }

            @Override
            public void dependencyAdded(TaskDependencyEvent e) {
                adjustDependencies(e.getDependency().getDependee());
                repaint();
            }

            @Override
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
                        e1.printStackTrace();
                    }
                }
            }
        });

        appli = app;

        getProject().getTaskCustomColumnManager().addListener(this);
    }

    /** @return the color of the task */
    public Color getTaskColor() {
        return myUIConfiguration.getTaskColor();
    }

    /** Change the color of the task */
    public void setProjectLevelTaskColor(Color c) {
        myUIConfiguration.setProjectLevelTaskColor(c);
    }

    /** @return the preferred size of the panel. */
    @Override
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

    @Override
    public String getName() {
        return GanttLanguage.getInstance().getText("gantt");
    }

    private int getHeaderHeight() {
        return getImplementation().getHeaderHeight(tree, tree.getTreeTable().getTable());
    }

    /** @return an image with the gantt chart */
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
        List<DefaultMutableTreeNode> visibleNodes = settings.isOnlySelectedItem() ?
            Arrays.asList(this.tree.getSelectedNodes()) :
            this.tree.getAllVisibleNodes();

        for (int i = 0; i < visibleNodes.size(); i++) {
            if (visibleNodes.get(i).isRoot()) {
                visibleNodes.remove(i);
                break;
            }
        }
        settings.setVisibleTasks(GanttTree2.convertNodesListToItemList(visibleNodes));
        return getRenderedImage(settings, tree.getTreeTable());
    }

    GPUndoManager getUndoManager() {
        return myUndoManager;
    }

    @Override
    protected ChartModelBase getChartModel() {
        return myChartModel;
    }

    @Override
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

    @Override
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
    }

    @Override
    public void repaint() {
        if (myChartModel != null && isShowing()) {
            myChartModel.setHeaderHeight(getHeaderHeight());
        }
        super.repaint();
    }

    @Override
    public void setBaseline(GanttPreviousState baseline) {
        if (baseline == null) {
            setPreviousStateTasks(null);
        } else {
            setPreviousStateTasks(baseline.load());
        }
        myBaseline = baseline;
    }

    @Override
    public GanttPreviousState getBaseline() {
        return myBaseline;
    }



    class MouseSupport {
        protected Task findTaskUnderMousePointer(int xpos, int ypos) {
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
    public interface ChartImplementation extends ZoomListener {
        void paintChart(Graphics g);

        void paintComponent(Graphics g, List<Task> visibleTasks);

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

//        void beginMoveTaskInteraction(MouseEvent e, Task task);

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
            setActiveInteraction(new ChangeTaskEndInteraction(
                initiatingEvent, taskBoundary,
                new TimelineFacadeImpl(getChartModel(), getTaskManager()),
                getUIFacade(),
                getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm()));
            setCursor(E_RESIZE_CURSOR);
        }

        public void beginChangeTaskStartInteraction(MouseEvent e,
                TaskBoundaryChartItem taskBoundary) {
            setActiveInteraction(new ChangeTaskStartInteraction(e, taskBoundary,
                new TimelineFacadeImpl(getChartModel(), getTaskManager()),
                getUIFacade(),
                getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm()));
            setCursor(W_RESIZE_CURSOR);
        }

        public void beginChangeTaskProgressInteraction(MouseEvent e,
                TaskProgressChartItem taskProgress) {
            setActiveInteraction(new ChangeTaskProgressInteraction(e, taskProgress,
                new TimelineFacadeImpl(getChartModel(), getTaskManager()),
                getUIFacade()));
            setCursor(CHANGE_PROGRESS_CURSOR);
        }

        public void beginDrawDependencyInteraction(MouseEvent initiatingEvent,
                TaskRegularAreaChartItem taskArea,
                GanttGraphicArea.MouseSupport mouseSupport) {
            setActiveInteraction(new DrawDependencyInteraction(initiatingEvent, taskArea,
                new TimelineFacadeImpl(getChartModel(), getTaskManager()),
                new DrawDependencyInteraction.ChartModelFacade() {
                    @Override
                    public Task findTaskUnderMousePointer(int xpos, int ypos) {
                        ChartItem chartItem = myChartModel.getChartItemWithCoordinates(xpos, ypos);
                        return chartItem == null ? null : chartItem.getTask();
                    }
                },
                getUIFacade(),
                getTaskManager().getDependencyCollection()));

        }

        public void beginMoveTaskInteractions(MouseEvent e, List<Task> tasks) {
            setActiveInteraction(new MoveTaskInteractions(e, tasks,
                new TimelineFacadeImpl(getChartModel(), getTaskManager()),
                getUIFacade(),
                getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm()));
        }

        public void paintComponent(Graphics g, List<Task> visibleTasks) {
            synchronized(ChartModelBase.STATIC_MUTEX) {
                GanttGraphicArea.super.paintComponent(g);
                ChartModel model = myChartModel;
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

        @Override
        public void paintChart(Graphics g) {
            synchronized(ChartModelBase.STATIC_MUTEX) {
                //GanttGraphicArea.super.paintComponent(g);
                ChartModel model = myChartModel;
                model.setBottomUnitWidth(getViewState().getBottomUnitWidth());
                model.setRowHeight(((GanttTree2) tree).getTreeTable()
                        .getRowHeight());
                model.setTopTimeUnit(getViewState().getTopTimeUnit());
                model.setBottomTimeUnit(getViewState().getBottomTimeUnit());
                VisibleNodesFilter visibleNodesFilter = new VisibleNodesFilter();
                List<Task> visibleTasks = tree.getVisibleNodes(visibleNodesFilter);
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

        @Override
        public IStatus canPaste(ChartSelection selection) {
            return Status.OK_STATUS;
        }

        @Override
        public ChartSelection getSelection() {
            ChartSelectionImpl result = new ChartSelectionImpl() {
                @Override
                public boolean isEmpty() {
                    return false;
                }
                @Override
                public void startCopyClipboardTransaction() {
                    super.startCopyClipboardTransaction();
                    tree.copySelectedNode();
                }
                @Override
                public void startMoveClipboardTransaction() {
                    super.startMoveClipboardTransaction();
                    tree.cutSelectedNode();
                }
            };
            return result;
        }

        @Override
        public void paste(ChartSelection selection) {
            tree.pasteNode();
        }


        private OldChartMouseListenerImpl myMouseListener = new OldChartMouseListenerImpl();

        private OldMouseMotionListenerImpl myMouseMotionListener = new OldMouseMotionListenerImpl();

    }

    @Override
    protected AbstractChartImplementation getImplementation() {
        return (AbstractChartImplementation) getChartImplementation();
    }

    private ChartImplementation getChartImplementation() {
        if (myChartComponentImpl == null) {
            myChartComponentImpl = new NewChartComponentImpl(getProject(), getChartModel(), this);
        }
        return myChartComponentImpl;
    }

    public void setPreviousStateTasks(List<GanttPreviousStateTask> tasks) {
        int rowHeight = myChartModel.setBaseline(tasks);
        appli.getTree().getTable().setRowHeight(rowHeight);
    }

    private ChartImplementation myChartComponentImpl;

    private class OldChartMouseListenerImpl extends MouseListenerBase {
        private MouseSupport myMouseSupport = new MouseSupport();

        private TaskSelectionManager getTaskSelectionManager() {
            return getUIFacade().getTaskSelectionManager();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Task taskUnderPointer = myMouseSupport.findTaskUnderMousePointer(e.getX(), e.getY());
                if (taskUnderPointer == null) {
                    getTaskSelectionManager().clear();
                }
            }
            if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                appli.propertiesTask();
            }
        }

        @Override
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

        @Override
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

        @Override
        public void mousePressed(MouseEvent e) {
            tree.stopEditing();
            super.mousePressed(e);
            Task taskUnderPointer = myMouseSupport.findTaskUnderMousePointer(e.getX(), e.getY());
            if (taskUnderPointer == null) {
                return;
            }
            if (taskUnderPointer!=null && !getTaskSelectionManager().isTaskSelected(taskUnderPointer)) {
                boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
                if (!ctrl) {
                    getTaskSelectionManager().clear();
                }
                getTaskSelectionManager().addTask(taskUnderPointer);
            }
            if (e.getButton() == MouseEvent.BUTTON2) {
                if (!getTaskSelectionManager().isTaskSelected(taskUnderPointer)) {
                    getTaskSelectionManager().clear();
                    getTaskSelectionManager().addTask(taskUnderPointer);
                }
                List<Task> l = getTaskSelectionManager().getSelectedTasks();
                getChartImplementation().beginMoveTaskInteractions(e, l);
            }
        }
    }

    private class OldMouseMotionListenerImpl extends MouseMotionListenerBase {
        private MouseSupport myMouseSupport = new MouseSupport();

        // Move the move on the area
        @Override
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

    @Override
    public Icon getIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void customPropertyChange(CustomPropertyEvent event) {
        repaint();
    }

    public void setUIConfiguration(UIConfiguration configuration) {
        myUIConfiguration = configuration;
    }

    @Override
    public void projectModified() {
        // TODO Auto-generated method stub
    }

    @Override
    public void projectSaved() {
        // TODO Auto-generated method stub
    }

    @Override
    public void projectClosed() {
        repaint();
        setProjectLevelTaskColor(null);
        setPreviousStateTasks(null);
    }
    @Override
    public void projectOpened() {
    }
    @Override
    public void projectCreated() {
        // TODO Auto-generated method stub

    }
    @Override
    public ChartViewState getViewState() {
        return myViewState;
    }

}
