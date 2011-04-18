/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2004-2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.delay.DelayManager;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueDispatcher;
import net.sourceforge.ganttproject.plugins.PluginManager;
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

    private static TaskSelectionManager taskSelectionManager = null;

    private static GPUndoManager undoManager = null;

    private static DelayManager delayManager = null;

    private static PluginManager pluginManager = new PluginManager();

    private static List<ChangeValueDispatcher> changeValueDispatchers = new ArrayList<ChangeValueDispatcher>();

    /**
     * Registers the unique GanttProject instance.
     * 
     * @param gp
     *            The unique GanttProject instance.
     */
    public static void registerGanttProject(GanttProject gp) {
        ganttprojectSingleton = gp;
    }

    public static void registerTaskSelectionManager(
            TaskSelectionManager taskSelection) {
        taskSelectionManager = taskSelection;
    }

    public static void registerUndoManager(GPUndoManager undoMgr) {
        undoManager = undoMgr;
    }

    public static void registerDelayManager(DelayManager delayMgr) {
        delayManager = delayMgr;
    }

    public static void addChangeValueDispatcher(ChangeValueDispatcher dispatcher){
        changeValueDispatchers.add(dispatcher);
    }

    /** @return The unique GanttProject instance. */
    @Deprecated
    public static GanttProject getGanttProjectSingleton() {
        return ganttprojectSingleton;
    }

    @Deprecated
    public static TaskSelectionManager getTaskSelectionManager() {
        return taskSelectionManager;
    }

    @Deprecated
    public static GPUndoManager getUndoManager() {
        return undoManager;
    }

    @Deprecated
    public static DelayManager getDelayManager() {
        return delayManager;
    }

    public static PluginManager getPluginManager() {
        return pluginManager;
    }

    public static List<ChangeValueDispatcher> getChangeValueDispatchers(){
        return changeValueDispatchers;
    }
}
