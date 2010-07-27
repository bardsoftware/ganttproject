package net.sourceforge.ganttproject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * This interface represents a project as a logical business entity, without any
 * UI (except some configuration options :)
 *
 * @author bard
 */
public interface IGanttProject {

    String getProjectName();

    void setProjectName(String projectName);

    //
    String getDescription();

    void setDescription(String description);

    //
    String getOrganization();

    void setOrganization(String organization);

    //
    String getWebLink();

    void setWebLink(String webLink);

    //
    /**
     * Creates a new task and performs all necessary initialization procedures
     * such as changing properties of parent task, adjusting schedule, etc.
     */
    Task newTask();

    //
    GanttLanguage getI18n();

    UIConfiguration getUIConfiguration();

    HumanResourceManager getHumanResourceManager();

    RoleManager getRoleManager();

    TaskManager getTaskManager();

    TaskContainmentHierarchyFacade getTaskContainment();

    GPCalendar getActiveCalendar();

    TimeUnitStack getTimeUnitStack();

    void setModified();
    void setModified(boolean modified);

    void close();

    Document getDocument();
    void setDocument(Document document);
    DocumentManager getDocumentManager();

    void addProjectEventListener(ProjectEventListener listener);
    void removeProjectEventListener(ProjectEventListener listener);

    boolean isModified();

    void open(Document document) throws IOException;

    CustomPropertyManager getResourceCustomPropertyManager();

    CustomColumnsManager getTaskCustomColumnManager();

    CustomColumnsStorage getCustomColumnsStorage();

    List/*<GanttPreviousState*/ getBaselines();

    /**
     * Repaints the complete Resource panel
     */
    void repaintResourcePanel();
}