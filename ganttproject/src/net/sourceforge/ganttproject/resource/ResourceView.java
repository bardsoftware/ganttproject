/*
 * Created on 21.06.2003
 *
 */
package net.sourceforge.ganttproject.resource;

import java.awt.Color;

import net.sourceforge.ganttproject.task.Task;

/**
 * @author dbarashev
 */
public interface ResourceView {
    /**
     * Default resource color
     */
    static final Color DEFAULT_RESOURCE_COLOR = Task.DEFAULT_COLOR; 

    /**
     * Default overload resource color
     */
    static final Color OVERLOAD_RESOURCE_COLOR = new Color(229, 50, 50); 

    /**
     * Default underload resource color
     */
    static final Color UNDERLOAD_RESOURCE_COLOR = new Color(50, 229, 50); 

    /**
     * Default day off color
     */
    static final Color DEFAULT_DAYOFF_COLOR = new Color(0.9f, 1f, 0.17f); 
    
    /**
     * This method is called whenever new resource appears in the resource
     * manager
     * 
     * @param event
     *            Resource event object
     */
    public void resourceAdded(ResourceEvent event);

    public void resourcesRemoved(ResourceEvent event);

	public void resourceChanged(ResourceEvent e);
    
    public void resourceAssignmentsChanged(ResourceEvent e);
}
