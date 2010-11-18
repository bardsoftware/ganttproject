package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.resource.HumanResource;

/**
 * @author bard
 */
public interface ResourceAssignmentCollection extends
        MutableResourceAssignmentCollection {
    ResourceAssignment[] getAssignments();

    ResourceAssignment getAssignment(HumanResource resource);

    ResourceAssignmentMutator createMutator();

    HumanResource getCoordinator();
    void clear();
}

interface MutableResourceAssignmentCollection {
    ResourceAssignment addAssignment(HumanResource resource);

    void deleteAssignment(HumanResource resource);
}
