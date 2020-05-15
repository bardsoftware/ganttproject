package net.sourceforge.ganttproject.actions;

import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.IntegerOption;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.AppKt;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.action.resource.AssignmentToggleAction;
import net.sourceforge.ganttproject.action.resource.ResourceDeleteAction;
import net.sourceforge.ganttproject.action.resource.ResourceNewAction;
import net.sourceforge.ganttproject.action.task.TaskDeleteAction;
import net.sourceforge.ganttproject.action.task.TaskNewAction;
import net.sourceforge.ganttproject.action.zoom.ZoomActionSet;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.*;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceContext;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.TaskView;
import net.sourceforge.ganttproject.undo.GPUndoListener;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import javax.swing.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

public abstract class ActionTestCase extends TestCase {
    private UIFacade myUIFacade;
    private TaskSelectionManager myTaskSelectionManager;
    private GanttProject myGanttProject;
    private GanttTree2 myGanttTree;
    private TaskManager myTaskManager;
    private HumanResourceManager myResourceManager;
    private RoleManager myRoleManager;
    private GPUndoManager myUndoManager;
    private ResourceNewAction myResourceNewAction;
    private HumanResource[] myHumanResources;
    int lock = 1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        while(lock == 0){}
        lock = 0;
        AppKt.main(new String[] {});
        while(myGanttProject == null){
            myGanttProject = AppKt.getMainWindow().get();
        }
        myUndoManager = makeUndoManager();
        myGanttTree = myGanttProject.getTree();
        myUIFacade = myGanttProject.getUIFacade();
        myTaskManager = myGanttProject.getTaskManager();
        myResourceManager = myGanttProject.getHumanResourceManager();
        myRoleManager = myGanttProject.getRoleManager();
        myTaskSelectionManager = myGanttProject.getTaskSelectionManager();
        myHumanResources = new HumanResource[] {};
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myGanttProject.close();
        myGanttProject.setVisible(false);
        myGanttProject.dispose();
        myGanttProject = null;
        myUndoManager = null;
        myGanttTree = null;
        myUIFacade = null;
        myTaskManager = null;
        myResourceManager = null;
        myRoleManager = null;
        myTaskSelectionManager = null;
        myHumanResources = null;
        lock = 1;
    }

    private GPUndoManager makeUndoManager() {
        return new GPUndoManager() {
            @Override
            public void undoableEdit(String localizedName, Runnable runnableEdit) {
                runnableEdit.run();
            }

            @Override
            public boolean canUndo() {
                return false;
            }

            @Override
            public boolean canRedo() {
                return false;
            }

            @Override
            public void undo() throws CannotUndoException {

            }

            @Override
            public void redo() throws CannotRedoException {

            }

            @Override
            public String getUndoPresentationName() {
                return null;
            }

            @Override
            public String getRedoPresentationName() {
                return null;
            }

            @Override
            public void addUndoableEditListener(GPUndoListener listener) {

            }

            @Override
            public void removeUndoableEditListener(GPUndoListener listener) {

            }

            @Override
            public void die() {

            }
        };
    }

    private UIFacade makeUIFacade(){
        return new UIFacade() {
            @Override
            public IntegerOption getDpiOption() {
                return null;
            }

            @Override
            public GPOption<String> getLafOption() {
                return null;
            }

            @Override
            public ScrollingManager getScrollingManager() {
                return null;
            }

            @Override
            public ZoomManager getZoomManager() {
                return null;
            }

            @Override
            public ZoomActionSet getZoomActionSet() {
                return null;
            }

            @Override
            public GPUndoManager getUndoManager() {
                return myUndoManager;
            }

            @Override
            public void setLookAndFeel(GanttLookAndFeelInfo laf) {

            }

            @Override
            public GanttLookAndFeelInfo getLookAndFeel() {
                return null;
            }

            @Override
            public Choice showConfirmationDialog(String message, String title) {
                return Choice.YES;
            }

            @Override
            public void showPopupMenu(Component invoker, Action[] actions, int x, int y) {

            }

            @Override
            public void showPopupMenu(Component invoker, Collection<Action> actions, int x, int y) {

            }

            @Override
            public void showOptionDialog(int messageType, String message, Action[] actions) {

            }

            @Override
            public Dialog createDialog(Component content, Action[] buttonActions, String title) {
                buttonActions[0].actionPerformed(null);
                return new Dialog() {
                    @Override
                    public void show() {
                    }

                    @Override
                    public void hide() {

                    }

                    @Override
                    public void layout() {

                    }

                    @Override
                    public void center(Centering centering) {

                    }
                };
            }

            @Override
            public void setStatusText(String text) {

            }

            @Override
            public void showErrorDialog(String errorMessage) {

            }

            @Override
            public void showNotificationDialog(NotificationChannel channel, String message) {

            }

            @Override
            public void showSettingsDialog(String pageID) {

            }

            @Override
            public void showErrorDialog(Throwable e) {

            }

            @Override
            public NotificationManager getNotificationManager() {
                return null;
            }

            @Override
            public GanttChart getGanttChart() {
                return null;
            }

            @Override
            public TimelineChart getResourceChart() {
                return null;
            }

            @Override
            public Chart getActiveChart() {
                return null;
            }

            @Override
            public int getViewIndex() {
                return 0;
            }

            @Override
            public void setViewIndex(int viewIndex) {

            }

            @Override
            public int getGanttDividerLocation() {
                return 0;
            }

            @Override
            public void setGanttDividerLocation(int location) {

            }

            @Override
            public int getResourceDividerLocation() {
                return 0;
            }

            @Override
            public void setResourceDividerLocation(int location) {

            }

            @Override
            public void refresh() {

            }

            @Override
            public Frame getMainFrame() {
                return null;
            }

            @Override
            public Image getLogo() {
                return null;
            }

            @Override
            public void setWorkbenchTitle(String title) {

            }

            @Override
            public TaskView getCurrentTaskView() {
                return null;
            }

            @Override
            public TaskTreeUIFacade getTaskTree() {
                return null;
            }

            @Override
            public ResourceTreeUIFacade getResourceTree() {
                return myUIFacade.getResourceTree();
            }

            @Override
            public TaskSelectionManager getTaskSelectionManager() {
                return null;
            }

            @Override
            public TaskSelectionContext getTaskSelectionContext() {
                return null;
            }

            @Override
            public DefaultEnumerationOption<Locale> getLanguageOption() {
                return null;
            }

            @Override
            public GPOptionGroup[] getOptions() {
                return new GPOptionGroup[0];
            }

            @Override
            public void addOnUpdateComponentTreeUi(Runnable callback) {

            }
        };
    }

//    private TaskSelectionManager makeTaskSelectionManager(){
//        return new TaskSelectionManager(Suppliers.memoize(new Supplier<TaskManager>() {
//                public TaskManager get() {
//                    return getTaskManager();
//                }
//            }
//        ));
//    }

    protected TaskDeleteAction makeDeleteTaskAction(){
        return new TaskDeleteAction(getTaskManager(), getTaskSelectionManager(), makeUIFacade(), getGanttTree());
    }

    protected TaskNewAction makeNewTaskAction(){
        return new TaskNewAction(getGanttProject(), getUIFacade());
    }

    protected ResourceNewAction makeNewResourceAction(){
        myResourceNewAction = new ResourceNewAction(getHumanResourceManger(), getRoleManager(), getTaskManager(), makeUIFacade());
        return myResourceNewAction;
    }

    protected ResourceDeleteAction makeDeleteResourceAction(){
        ResourceContext context = new ResourceContext() {
            @Override
            public HumanResource[] getResources() {
                return myHumanResources;
            }
        };
        return new ResourceDeleteAction(getHumanResourceManger(), context, getGanttProject(), makeUIFacade());
    }

    protected AssignmentToggleAction makeAssignmentToggleAction(HumanResource resource, Task task){
        return new AssignmentToggleAction(resource, task, getUIFacade());
    }

//    protected GPUndoManager getUndoManager(){
//        return myUndoManager;
//    }

    protected TaskManager getTaskManager(){
        return myTaskManager;
    }

    protected UIFacade getUIFacade(){
        return myUIFacade;
    }

    protected TaskSelectionManager getTaskSelectionManager(){
        return myTaskSelectionManager;
    }

    protected GanttProject getGanttProject(){
        return myGanttProject;
    }

    protected GanttTree2 getGanttTree(){
        return myGanttTree;
    }

    protected HumanResourceManager getHumanResourceManger(){
        return myResourceManager;
    }

    protected RoleManager getRoleManager(){
        return myRoleManager;
    }

//    protected ResourceContext getResourceContext(){
//        return myResourceContext;
//    }

    protected Task createTask() {
        Task result = getTaskManager().createTask();
        result.move(getTaskManager().getRootTask());
        result.setName(String.valueOf(result.getTaskID()));
        return result;
    }

    protected void addHumanResourceToSelection(HumanResource resource){
        ArrayList<HumanResource> temp = new ArrayList<>(Arrays.asList(myHumanResources));
        temp.add(resource);
        myHumanResources = temp.toArray(myHumanResources);
    }

    protected void resetHumanResourceSelection(){
        myHumanResources = new HumanResource[] {};
    }
}
