// Copyright (C) 2021 BarD Software
package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.resource.HumanResource;

/**
 * @author dbarashev@bardsoftware.com
 */
interface MutableResourceAssignmentCollection {
  ResourceAssignment addAssignment(HumanResource resource);

  void deleteAssignment(HumanResource resource);
}
