/*
 * Created on 03.05.2005
 */
package net.sourceforge.ganttproject.chart;

import net.sourceforge.ganttproject.resource.ProjectResource;

/**
 * @author bard
 */
public interface ResourceChart extends Chart {
    boolean isExpanded(ProjectResource resource);
}
