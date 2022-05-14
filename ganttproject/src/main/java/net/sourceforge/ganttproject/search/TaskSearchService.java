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

import biz.ganttproject.customproperty.CustomProperty;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Search service for tasks */
public class TaskSearchService extends SearchServiceBase<TaskSearchService.MySearchResult, Task> {
  static class MySearchResult extends SearchResult<Task> {
    MySearchResult(Task t, TaskSearchService searchService, String query, String snippet, String snippetText) {
      super(t.getTaskID(), GanttLanguage.getInstance().getText("generic.task"), t.getName(), query, snippet, snippetText, t, searchService);
    }
  }

  public TaskSearchService() {
    super(UIFacade.GANTT_INDEX);
  }

  @Override
  public List<MySearchResult> search(String query) {
    query = query.toLowerCase();
    List<MySearchResult> results = new ArrayList<>();
    for (Task t : getProject().getTaskManager().getTasks()) {
      String snippet = "";
      String snippetText = "";
      boolean matched = false;
      if (isNotEmptyAndContains(t.getName(), query)) {
        matched = true;
      }
      for (CustomProperty c : t.getCustomValues().getCustomProperties()) {
        if (isNotEmptyAndContains(c.getValueAsString(), query)) {
          matched = true;
          snippet = c.getDefinition().getName();
          snippetText = c.getValueAsString();
          break;
        }
      }
      if (isNotEmptyAndContains(t.getNotes(), query)) {
        matched = true;
        snippet = GanttLanguage.getInstance().getText("notes");
        snippetText = t.getNotes();
      }
      if (isNotEmptyAndContains(String.valueOf(t.getTaskID()), query)) {
        matched = true;
        snippet = GanttLanguage.getInstance().getText("id");
        snippetText = String.valueOf(t.getTaskID());
      }
      if (matched) {
        results.add(new MySearchResult(t, this, query, snippet, snippetText));
      }
    }
    return results;
  }

  @Override
  public void init(IGanttProject project, UIFacade uiFacade) {
    super.init(project, null, uiFacade);
  }

  @Override
  public void select(List<MySearchResult> list) {
    var taskSelectionManager = getUiFacade().getTaskSelectionManager();
    taskSelectionManager.setUserInputConsumer(this);
    taskSelectionManager.setSelectedTasks(
        list.stream().map(searchResult -> searchResult.getObject()).collect(Collectors.toList()), this);
  }
}
