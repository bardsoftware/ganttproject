package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelImpl;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.PublicHolidayDialogAction;
import net.sourceforge.ganttproject.chart.export.RenderedChartImage;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionChangeListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomPropertyEvent;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskScheduleAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.event.TaskDependencyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;
import net.sourceforge.ganttproject.time.gregorian.GregorianCalendar;
import net.sourceforge.ganttproject.undo.GPUndoManager;


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

    static final Cursor W_RESIZE_CURSOR = new Cursor(
            Cursor.W_RESIZE_CURSOR);

    static final Cursor E_RESIZE_CURSOR = new Cursor(
            Cursor.E_RESIZE_CURSOR);

    static final Cursor CHANGE_PROGRESS_CURSOR;

    public GanttTree2 tree;

    public static Color taskDefaultColor = new Color(140, 182, 206);

    private GanttProject appli;

    private UIConfiguration myUIConfiguration;

    private final ChartModelImpl myChartModel;

    private final TaskManager myTaskManager;

    private GPUndoManager myUndoManager;

    private ChartViewState myViewState;

    private GanttPreviousState myBaseline;

    private final PublicHolidayDialogAction myPublicHolidayDialogAction;

    public GanttGraphicArea(GanttProject app, GanttTree2 ttree, TaskManager taskManager, ZoomManager zoomManager,
            GPUndoManager undoManager) {
        super(app.getProject(), app.getUIFacade(), zoomManager);
        this.setBackground(Color.WHITE);
        myTaskManager = taskManager;
        myUndoManager = undoManager;
        appli = app;

        myChartModel = new ChartModelImpl(getTaskManager(), app.getTimeUnitStack(), app.getUIConfiguration());
        myChartModel.addOptionChangeListener(new GPOptionChangeListener() {
            public void optionsChanged() {
                repaint();

            }
        });
        this.tree = ttree;
        myViewState = new ChartViewState(this, app.getUIFacade());
        app.getUIFacade().getZoomManager().addZoomListener(myViewState);

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

        myPublicHolidayDialogAction = new PublicHolidayDialogAction(getProject(), getUIFacade());

        getProject().getTaskCustomColumnManager().addListener(this);
        initMouseListeners();
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

    @Override
    protected GPTreeTableBase getTreeTable() {
        return tree.getTreeTable();
    }

    @Override
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
        return super.getRenderedImage(settings);
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

    @Override
    public Action[] getPopupMenuActions() {
        return new Action[] { getOptionsDialogAction(), myPublicHolidayDialogAction };
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



    static class MouseSupport {
        private final ChartModelImpl myChartModel;

        MouseSupport(ChartModelImpl chartModel) {
            myChartModel = chartModel;
        }
        protected Task findTaskUnderMousePointer(int xpos, int ypos) {
            ChartItem chartItem = myChartModel.getChartItemWithCoordinates(xpos, ypos);
            return chartItem == null ? null : chartItem.getTask();
        }

        protected ChartItem getChartItemUnderMousePoint(int xpos, int ypos) {
            ChartItem result = myChartModel.getChartItemWithCoordinates(xpos, ypos);
            return result;
        }
    }
    @Override
    protected AbstractChartImplementation getImplementation() {
        return (AbstractChartImplementation) getChartImplementation();
    }

    NewChartComponentImpl getChartImplementation() {
        if (myChartComponentImpl == null) {
            myChartComponentImpl = new NewChartComponentImpl(
                    getProject(), getUIFacade(), myChartModel, this, tree, getViewState());
        }
        return myChartComponentImpl;
    }

    public void setPreviousStateTasks(List<GanttPreviousStateTask> tasks) {
        int rowHeight = myChartModel.setBaseline(tasks);
        appli.getTree().getTable().setRowHeight(rowHeight);
    }

    private NewChartComponentImpl myChartComponentImpl;

    static class OldChartMouseListenerImpl extends MouseListenerBase {
        private final GanttTree2 myTree;
        private final NewChartComponentImpl myChartImplementation;
        private final UIFacade myUiFacade;
        private final ChartComponentBase myChartComponent;

        public OldChartMouseListenerImpl(
                NewChartComponentImpl chartImplementation, ChartModelImpl chartModel, UIFacade uiFacade, ChartComponentBase chartComponent, GanttTree2 tree) {
            super(uiFacade, chartComponent, chartImplementation);
            myUiFacade = uiFacade;
            myTree = tree;
            myChartImplementation = chartImplementation;
            myChartComponent = chartComponent;
        }
        private TaskSelectionManager getTaskSelectionManager() {
            return myUiFacade.getTaskSelectionManager();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Task taskUnderPointer = myChartImplementation.findTaskUnderPointer(e.getX(), e.getY());
                if (taskUnderPointer == null) {
                    getTaskSelectionManager().clear();
                }
            }
            if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                myTree.getTaskPropertiesAction().actionPerformed(null);
            }
        }

        @Override
        protected Action[] getPopupMenuActions() {
            Action[] treeActions = myTree.getPopupMenuActions();
            int sep = 0;
            if (treeActions.length != 0) {
                sep = 1;
            }

            Action[] chartActions = myChartComponent.getPopupMenuActions();
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
            ChartItem itemUnderPoint = myChartImplementation.getChartItemUnderMousePoint(e.getX(), e.getY());
            if (itemUnderPoint instanceof TaskBoundaryChartItem) {
                TaskBoundaryChartItem taskBoundary = (TaskBoundaryChartItem) itemUnderPoint;
                if (taskBoundary.isStartBoundary()) {
                    myChartImplementation.beginChangeTaskStartInteraction(e, taskBoundary);
                }
                else {
                    myChartImplementation.beginChangeTaskEndInteraction(e, taskBoundary);
                }
            }
            else if (itemUnderPoint instanceof TaskProgressChartItem) {
                myChartImplementation.beginChangeTaskProgressInteraction(
                        e, (TaskProgressChartItem) itemUnderPoint);
            }
            else if (itemUnderPoint instanceof TaskRegularAreaChartItem) {
                myChartImplementation.beginDrawDependencyInteraction(
                        e, (TaskRegularAreaChartItem) itemUnderPoint);
            }
            else {
                isMineEvent = false;
                super.processLeftButton(e);
            }
            if (isMineEvent) {
                myUiFacade.refresh();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            myTree.stopEditing();
            super.mousePressed(e);
            Task taskUnderPointer = myChartImplementation.findTaskUnderPointer(e.getX(), e.getY());
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
                myChartImplementation.beginMoveTaskInteractions(e, l);
            }
        }
    }

    static class OldMouseMotionListenerImpl extends MouseMotionListenerBase {
        private final MouseSupport myMouseSupport;
        private final ChartComponentBase myChartComponent;

        public OldMouseMotionListenerImpl(NewChartComponentImpl chartImplementation, ChartModelImpl chartModel, UIFacade uiFacade, ChartComponentBase chartComponent) {
            super(uiFacade, chartImplementation);
            myMouseSupport = new MouseSupport(chartModel);
            myChartComponent = chartComponent;
        }
        // Move the move on the area
        @Override
        public void mouseMoved(MouseEvent e) {
            ChartItem itemUnderPoint = myMouseSupport
                    .getChartItemUnderMousePoint(e.getX(), e.getY());
            Task taskUnderPoint = itemUnderPoint == null ? null : itemUnderPoint.getTask();
            // System.err.println("[OldMouseMotionListenerImpl] mouseMoved:
            // taskUnderPoint="+taskUnderPoint);
            if (taskUnderPoint == null) {
                myChartComponent.setDefaultCursor();
            } else {
                if (itemUnderPoint instanceof TaskBoundaryChartItem) {
                    Cursor cursor = ((TaskBoundaryChartItem) itemUnderPoint)
                            .isStartBoundary() ? W_RESIZE_CURSOR
                            : E_RESIZE_CURSOR;
                    myChartComponent.setCursor(cursor);
                }
                // special cursor
                else if (itemUnderPoint instanceof TaskProgressChartItem) {
                    myChartComponent.setCursor(CHANGE_PROGRESS_CURSOR);
                } else {
                    myChartComponent.setDefaultCursor();
                }
                getUIFacade().refresh();
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
