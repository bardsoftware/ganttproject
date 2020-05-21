package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.chart.scene.SceneBuilder;
import biz.ganttproject.core.option.*;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import net.sourceforge.ganttproject.*;
import net.sourceforge.ganttproject.action.resource.AssignmentToggleAction;
import net.sourceforge.ganttproject.action.resource.ResourceNewAction;
import net.sourceforge.ganttproject.action.task.TaskNewAction;
import net.sourceforge.ganttproject.action.zoom.ZoomActionSet;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import net.sourceforge.ganttproject.gui.*;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.TaskView;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import org.eclipse.core.runtime.IStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.*;
import java.util.List;

public abstract class RenderTestCase extends GlobalTestLock {
    private GanttProject myGanttProject;
    private HumanResource[] myHumanResources;
    private ArrayList<biz.ganttproject.core.chart.canvas.Canvas.Rectangle> shapes = new ArrayList<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myGanttProject = GlobalTestLock.myproject;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myGanttProject = null;
    }

    protected HumanResourceManager getHumanResourceManger(){
        return myGanttProject.getHumanResourceManager();
    }

    protected TaskManager getTaskManager(){
        return myGanttProject.getTaskManager();
    }

    protected ResourceLoadRenderer makeResourceLoadRenderer(){
        ResourceChart chart = new ResourceChart() {
                    @Override
                    public boolean isExpanded(HumanResource resource) {
                        return false;
                    }

                    @Override
                    public IGanttProject getProject() {
                        return null;
                    }

                    @Override
                    public void init(IGanttProject project, IntegerOption dpiOption, FontOption chartFontOption) {

                    }

                    @Override
                    public void buildImage(GanttExportSettings settings, ChartImageVisitor imageVisitor) {

                    }

                    @Override
                    public RenderedImage getRenderedImage(GanttExportSettings settings) {
                        return null;
                    }

                    @Override
                    public Date getStartDate() {
                        return null;
                    }

                    @Override
                    public void setStartDate(Date startDate) {

                    }

                    @Override
                    public Date getEndDate() {
                        return null;
                    }

                    @Override
                    public void setDimensions(int height, int width) {

                    }

                    @Override
                    public String getName() {
                        return null;
                    }

                    @Override
                    public void reset() {

                    }

                    @Override
                    public GPOptionGroup[] getOptionGroups() {
                        return new GPOptionGroup[0];
                    }

                    @Override
                    public Chart createCopy() {
                        return null;
                    }

                    @Override
                    public ChartSelection getSelection() {
                        return null;
                    }

                    @Override
                    public IStatus canPaste(ChartSelection selection) {
                        return null;
                    }

                    @Override
                    public void paste(ChartSelection selection) {

                    }

                    @Override
                    public void addSelectionListener(ChartSelectionListener listener) {

                    }

                    @Override
                    public void removeSelectionListener(ChartSelectionListener listener) {

                    }

                    @Override
                    public Object getAdapter(Class aClass) {
                        return null;
                    }
                };
        ChartModelResource resource = new ChartModelResource(
                myGanttProject.getTaskManager(),
                myGanttProject.getHumanResourceManager(),
                myGanttProject.getTimeUnitStack(),
                myGanttProject.getUIConfiguration(),
                chart);
        resource.setBottomTimeUnit(myGanttProject.getTimeUnitStack().getDefaultTimeUnit());
        resource.setStartDate(new Date(120, Calendar.MAY, 20));
        resource.setBounds(new Dimension(200, 200));
        resource.setTopTimeUnit(GPTimeUnitStack.WEEK);
        resource.setBottomTimeUnit(GPTimeUnitStack.DAY);
        resource.setBottomUnitWidth(20);
        resource.setRowHeight(10);
        return new ResourceLoadRenderer(resource, chart);
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
                return myGanttProject.getUndoManager();
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
                return myGanttProject.getUIFacade().getResourceTree();
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


    protected TaskNewAction makeNewTaskAction(){
        return new TaskNewAction(myGanttProject, myGanttProject.getUIFacade());
    }

    protected AssignmentToggleAction makeAssignmentToggleAction(HumanResource resource, Task task){
        return new AssignmentToggleAction(resource, task, myGanttProject.getUIFacade());
    }

    protected ResourceNewAction makeNewResourceAction(){
        return new ResourceNewAction(
                myGanttProject.getHumanResourceManager(),
                myGanttProject.getRoleManager(),
                myGanttProject.getTaskManager(),
                makeUIFacade());
    }

    protected List<SceneBuilder> get_renderers(){
        return null;
    }

    protected GanttProject getMyGanttProject(){
        return myGanttProject;
    }

    protected void addHumanResourceToSelection(HumanResource resource){
        ArrayList<HumanResource> temp = new ArrayList<>(Arrays.asList(myHumanResources));
        temp.add(resource);
        myHumanResources = temp.toArray(myHumanResources);
    }

    protected void resetHumanResourceSelection(){
        myHumanResources = new HumanResource[] {};
    }

    protected biz.ganttproject.core.chart.canvas.Painter makePainter(){
        return new biz.ganttproject.core.chart.canvas.Painter() {
            @Override
            public void prePaint() {

            }

            @Override
            public void paint(biz.ganttproject.core.chart.canvas.Canvas.Rectangle rectangle) {
                shapes.add(rectangle);
            }

            @Override
            public void paint(biz.ganttproject.core.chart.canvas.Canvas.Line line) {

            }

            @Override
            public void paint(biz.ganttproject.core.chart.canvas.Canvas.Text next) {

            }

            @Override
            public void paint(biz.ganttproject.core.chart.canvas.Canvas.TextGroup textGroup) {

            }

            @Override
            public void paint(biz.ganttproject.core.chart.canvas.Canvas.Polygon p) {

            }
        };
    }

    protected List<biz.ganttproject.core.chart.canvas.Canvas.Rectangle> getShapes(){
        return shapes;
    }
}
