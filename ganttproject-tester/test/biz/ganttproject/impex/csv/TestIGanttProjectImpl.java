package biz.ganttproject.impex.csv;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.time.TimeUnitStack;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ProjectEventListener;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: akurutin
 * Date: 03.04.17
 * Time: 20:24
 */
//TODO: use mock class instead of hack
public class TestIGanttProjectImpl implements IGanttProject {
    private TaskManager taskManager;
    private HumanResourceManager hrManager;
    private RoleManager roleManager;

    public TestIGanttProjectImpl(TaskManager taskManager, HumanResourceManager hrManager, RoleManager roleManager) {
        this.taskManager = taskManager;
        this.hrManager = hrManager;
        this.roleManager = roleManager;
    }

    @Override
    public String getProjectName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProjectName(String projectName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDescription(String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getOrganization() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOrganization(String organization) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getWebLink() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWebLink(String webLink) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UIConfiguration getUIConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HumanResourceManager getHumanResourceManager() {
        return hrManager;
    }

    @Override
    public RoleManager getRoleManager() {
        return roleManager;
    }

    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public TaskContainmentHierarchyFacade getTaskContainment() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GPCalendarCalc getActiveCalendar() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TimeUnitStack getTimeUnitStack() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setModified(boolean modified) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Document getDocument() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDocument(Document document) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentManager getDocumentManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addProjectEventListener(ProjectEventListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProjectEventListener(ProjectEventListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void open(Document document) throws IOException, Document.DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CustomPropertyManager getResourceCustomPropertyManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CustomPropertyManager getTaskCustomColumnManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<GanttPreviousState> getBaselines() {
        throw new UnsupportedOperationException();
    }
}
