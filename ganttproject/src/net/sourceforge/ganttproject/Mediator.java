package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.delay.DelayManager;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueDispatcher;
import net.sourceforge.ganttproject.plugins.PluginManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.undo.GPUndoManager;

/**
 * This class allow the developer to get some useful references. - GanttProject
 * reference; - CustomColumnManager reference; - CustomColumnStorage reference.
 * 
 * @author bbaranne Mar 2, 2005
 */
public class Mediator {
    /**
     * The unique GanttProject instance.
     */
    private static GanttProject ganttprojectSingleton = null;

    /**
     * The unique CustomColumnsManager instance.
     */
    private static CustomColumnsManager customColumnsManager = null;

    private static CustomColumnsStorage customColumnsStorage = null;

    private static TaskSelectionManager taskSelectionManager = null;

    private static RoleManager roleManager = null;

    private static TaskManager taskManager = null;

    private static GPUndoManager undoManager = null;

    private static DelayManager delayManager = null;

    private static PluginManager pluginManager = new PluginManager();
    
    private static List changeValueDispatchers = new ArrayList();

    /**
     * Regsiters the unique GanttProject instance.
     * 
     * @param gp
     *            The unique GanttProject instance.
     */
    public static void registerGanttProject(GanttProject gp) {
        ganttprojectSingleton = gp;
    }

    /**
     * Regsiters the unique CustomColumnsManager instance.
     * 
     * @param managerThe
     *            unique CustomColumnsManager instance.
     */
//    public static void registerCustomColumnsManager(CustomColumnsManager manager) {
//        customColumnsManager = manager;
//    }
//
//    public static void registerCustomColumnsStorage(CustomColumnsStorage storage) {
//        customColumnsStorage = storage;
//    }
//
    public static void registerTaskSelectionManager(
            TaskSelectionManager taskSelection) {
        taskSelectionManager = taskSelection;
    }

//    public static void registerRoleManager(RoleManager roleMgr) {
//        roleManager = roleMgr;
//    }
//
//    public static void registerTaskManager(TaskManager taskMgr) {
//        taskManager = taskMgr;
//    }

    public static void registerUndoManager(GPUndoManager undoMgr) {
        undoManager = undoMgr;
    }

    public static void registerDelayManager(DelayManager delayMgr) {
        delayManager = delayMgr;
    }
    
    public static void addChangeValueDispatcher(ChangeValueDispatcher dispatcher){
        changeValueDispatchers.add(dispatcher);
    }

    /**
     * Returns the unique GanttProject instance.
     * 
     * @return The unique GanttProject instance.
     */
    public static GanttProject getGanttProjectSingleton() {
        return ganttprojectSingleton;
    }

    /**
     * Returns the unique CustomColumnsStorage instance.
     * 
     * @return The unique CustomColumnsStorage instance.
     */
//    public static CustomColumnsStorage getCustomColumnsStorage() {
//        return customColumnsStorage;
//    }

    /**
     * Returns the unique CustomColumnsManager instance.
     * 
     * @return The unique CustomColumnsManager instance.
     */
//    public static CustomColumnsManager getCustomColumnsManager() {
//        return customColumnsManager;
//    }

    public static TaskSelectionManager getTaskSelectionManager() {
        return taskSelectionManager;
    }

//    public static RoleManager getRoleManager() {
//        return roleManager;
//    }

//    public static TaskManager getTaskManager() {
//        return taskManager;
//    }

    public static GPUndoManager getUndoManager() {
        return undoManager;
    }

    public static DelayManager getDelayManager() {
        return delayManager;
    }

    public static PluginManager getPluginManager() {
        return pluginManager;
    }
    
    public static List getChangeValueDispatchers(){
        return changeValueDispatchers;
    }
}
