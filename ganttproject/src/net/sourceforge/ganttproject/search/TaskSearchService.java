/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.search;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;

/** Search service for tasks */
public class TaskSearchService extends SearchServiceBase<TaskSearchService.MySearchResult, Task> {
  static class MySearchResult extends SearchResult<Task> {
    public MySearchResult(Task t, TaskSearchService searchService) {
      super("Task: " + t.getName(), "", "", t, searchService);
    }
  }

  public TaskSearchService() {
    super(UIFacade.GANTT_INDEX);
  }

  @Override
  public List<MySearchResult> search(String query) {
    query = query.toLowerCase();
    List<MySearchResult> results = new ArrayList<MySearchResult>();
    for (Task t : getProject().getTaskManager().getTasks()) {
      if (isNotEmptyAndContains(t.getName(), query) || isNotEmptyAndContains(t.getNotes(), query)
          || isNotEmptyAndContains(String.valueOf(t.getTaskID()), query)) {
        results.add(new MySearchResult(t, this));
      }
    }
    return results;
  }

  @Override
  public void init(IGanttProject project, UIFacade uiFacade) {
    super.init(project, uiFacade.getTaskTree(), uiFacade);
  }

}
