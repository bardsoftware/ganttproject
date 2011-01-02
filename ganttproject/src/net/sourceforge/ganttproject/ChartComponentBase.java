package net.sourceforge.ganttproject;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartRendererBase;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.chart.ChartUIConfiguration;
import net.sourceforge.ganttproject.chart.ChartViewState;
import net.sourceforge.ganttproject.chart.OptionsDialogAction;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.mouse.TimelineFacadeImpl;
import net.sourceforge.ganttproject.chart.mouse.MouseInteraction;
import net.sourceforge.ganttproject.chart.mouse.ScrollViewInteraction;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitStack;

public abstract class ChartComponentBase extends JPanel implements TimelineChart {
    private static final Cursor DEFAULT_CURSOR = Cursor
            .getPredefinedCursor(Cursor.HAND_CURSOR);

    //protected final ChartViewState myChartViewState;

    private final IGanttProject myProject;

    private final ZoomManager myZoomManager;

    private MouseWheelListenerBase myMouseWheelListener;

    private final UIFacade myUIFacade;

    private OptionsDialogAction myOptionsDialogAction;

    public ChartComponentBase(IGanttProject project, UIFacade uiFacade,
            ZoomManager zoomManager) {
        myProject = project;
        myUIFacade = uiFacade;
        myZoomManager = zoomManager;
        //myChartViewState = new ChartViewState(this, uiFacade);
        myMouseWheelListener = new MouseWheelListenerBase();
        addMouseListener(getMouseListener());
        addMouseMotionListener(getMouseMotionListener());
        addMouseWheelListener(myMouseWheelListener);
    }

    public Object getAdapter(Class adapter) {
        if (Component.class.isAssignableFrom(adapter)) {
            return this;
        }
        return null;
    }

    public abstract ChartViewState getViewState();

    public ZoomListener getZoomListener() {
        return getImplementation();
    }
    public ZoomManager getZoomManager(){
        return myZoomManager;
    }


    public GPOptionGroup[] getOptionGroups() {
        return getChartModel().getChartOptionGroups();
    }

    public Chart createCopy() {
        return new AbstractChartImplementation(myProject, getChartModel().createCopy(), this);
    }

    public ChartSelection getSelection() {
        return getImplementation().getSelection();
    }

    public IStatus canPaste(ChartSelection selection) {
        return getImplementation().canPaste(selection);
    }

    public void paste(ChartSelection selection) {
        getImplementation().paste(selection);
    }

    public void addSelectionListener(ChartSelectionListener listener) {
        getImplementation().addSelectionListener(listener);
    }
    public void removeSelectionListener(ChartSelectionListener listener) {
        getImplementation().removeSelectionListener(listener);
    }

    protected UIFacade getUIFacade() {
        return myUIFacade;
    }

    protected TaskManager getTaskManager() {
        return myProject.getTaskManager();
    }

    protected TimeUnitStack getTimeUnitStack() {
        return myProject.getTimeUnitStack();
    }

    protected UIConfiguration getUIConfiguration() {
        return myProject.getUIConfiguration();
    }

    protected void setDefaultCursor() {
        setCursor(DEFAULT_CURSOR);
    }

    public Action getOptionsDialogAction() {
        if (myOptionsDialogAction == null) {
            myOptionsDialogAction = new OptionsDialogAction(getOptionGroups(), getUIFacade());
        }
        return myOptionsDialogAction;
    }

    protected Component createPreviewComponent() {
        return null;
    }

    public ChartModel getModel() {
        return getChartModel();
    }
    
    public ChartUIConfiguration getStyle() {
        return getChartModel().getChartUIConfiguration();
    }
    
    protected abstract ChartModelBase getChartModel();

    protected abstract MouseListener getMouseListener();

    protected abstract MouseMotionListener getMouseMotionListener();

    // protected abstract MouseWheelListener getMouseWheelListener();

    protected class MouseListenerBase extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                Action[] actions = getPopupMenuActions();
                if (actions.length>0) {
                    getUIFacade().showPopupMenu(ChartComponentBase.this, actions,
                            e.getX(), e.getY());
                }
                return;
            }
            switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                processLeftButton(e);
                break;
            }
        }

        protected void processLeftButton(MouseEvent e) {
            getImplementation().beginScrollViewInteraction(e);
            ChartComponentBase.this.requestFocus();
        }

        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            getImplementation().finishInteraction();
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        public void mouseEntered(MouseEvent e) {
            setDefaultCursor();
        }

        public void mouseExited(MouseEvent e) {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        protected Action[] getPopupMenuActions() {
            return new Action[0];
        }
    }

    protected class MouseMotionListenerBase extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent e) {
            super.mouseDragged(e);
            MouseInteraction activeInteraction = getImplementation()
                    .getActiveInteraction();
            if (activeInteraction != null) {
                activeInteraction.apply(e);
                // myUIFacade.repaint2();
                // e.consume();
                // return;
            }
        }
    }

    protected class MouseWheelListenerBase implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (isRotationUp(e)) {
                fireZoomOut();
            } else {
                fireZoomIn();
            }
        }

        private void fireZoomIn() {
            if (myZoomManager.canZoomIn()) {
                myZoomManager.zoomIn();
//              reset the block size of the chart scrollbar
            }
        }

        private void fireZoomOut() {
            if (myZoomManager.canZoomOut()) {
                myZoomManager.zoomOut();
//              reset the block size of the chart scrollbar
            }
        }

        private boolean isRotationUp(MouseWheelEvent e) {
            return e.getWheelRotation() < 0;
        }
    }

    protected abstract AbstractChartImplementation getImplementation();

    public Date getStartDate() {
        return getImplementation().getStartDate();
    }

    public void setStartDate(Date startDate) {
        getImplementation().setStartDate(startDate);
        repaint();
    }

    public IGanttProject getProject() {
        return myProject;
    }

    public Date getEndDate() {
        return getImplementation().getEndDate();
    }

    public void scrollBy(TaskLength duration) {
        getImplementation().scrollBy(duration);
        repaint();
    }

    @Override
    public void setStartOffset(int pixels) {
        getImplementation().setStartOffset(pixels);
        repaint();
    }

    public void setDimensions(int height, int width) {
        getImplementation().setDimensions(height, width);
    }

    public void setBottomUnit(TimeUnit bottomUnit) {
        getImplementation().setBottomUnit(bottomUnit);
    }
    public void setTopUnit(TimeUnit topUnit) {
        getImplementation().setTopUnit(topUnit);
    }

    public void setBottomUnitWidth(int width) {
        getImplementation().setBottomUnitWidth(width);
    }
    public void paintChart(Graphics g) {
        getImplementation().paintChart(g);
    }
    public void addRenderer(ChartRendererBase renderer) {
        getImplementation().addRenderer(renderer);
    }
//    public void addTimeUnitVisitor(TimeUnitVisitor visitor) {
//        getImplementation().addTimeUnitVisitor(visitor);
//    }
    public void resetRenderers() {
        getImplementation().resetRenderers();
    }
    public TaskLength calculateLength(int x) {
        return getImplementation().calculateLength(x);
    }

    /** draw the panel */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        getChartModel().setBounds(getSize());
        getImplementation().paintChart(g);
    }

    protected static class ChartSelectionImpl implements ChartSelection {
        private List<Task> myTasks = new ArrayList<Task>();
        private List<Task> myTasksRO = Collections.unmodifiableList(myTasks);
        private List<HumanResource> myHumanResources = new ArrayList<HumanResource>();
        private List<HumanResource> myHumanResourceRO = Collections.unmodifiableList(myHumanResources);
        private boolean isTransactionRunning;

        public boolean isEmpty() {
            return myTasks.isEmpty() && myHumanResources.isEmpty();
        }

        public List<Task> getTasks() {
            return myTasksRO;
        }

        public List<HumanResource> getHumanResources() {
            return myHumanResourceRO;
        }

        public IStatus isDeletable() {
            return Status.OK_STATUS;
        }

        public void startCopyClipboardTransaction() {
            if (isTransactionRunning) {
                throw new IllegalStateException("Transaction is already running");
            }
            isTransactionRunning = true;
        }
        public void startMoveClipboardTransaction() {
            if (isTransactionRunning) {
                throw new IllegalStateException("Transaction is already running");
            }
            isTransactionRunning = true;
        }
        public void cancelClipboardTransaction() {
            isTransactionRunning = false;
        }
        public void commitClipboardTransaction() {
            isTransactionRunning = false;
        }

    }

    public MouseInteraction newScrollViewInteraction(MouseEvent e) {
        return new ScrollViewInteraction(
            e, new TimelineFacadeImpl(getChartModel(), getTaskManager()), getUIFacade().getScrollingManager(), 
            getChartModel().getBottomUnit());
    }
}