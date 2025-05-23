/*
Copyright 2003-2012 GanttProject Team

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

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Search service for resources */
public class ResourceSearchService extends SearchServiceBase<ResourceSearchService.MySearchResult, HumanResource> {
  static class MySearchResult extends SearchResult<HumanResource> {
    MySearchResult(HumanResource hr, ResourceSearchService searchService, String query, String snippet, String snippetText) {
      super(hr.getId(), GanttLanguage.getInstance().getText("generic.resource"), hr.getName(), query, snippet, snippetText, hr, searchService);
    }
  }

  public ResourceSearchService() {
    super(UIFacade.RESOURCES_INDEX);
  }

  @Override
  public List<MySearchResult> search(String query) {
    query = query.toLowerCase();
    List<MySearchResult> results = new ArrayList<>();
    for (HumanResource hr : getProject().getHumanResourceManager().getResources()) {
      if (isNotEmptyAndContains(hr.getName(), query)) {
        results.add(new MySearchResult(hr, this, query, "", ""));
      }
    }
    return results;
  }

  @Override
  public void init(IGanttProject project, UIFacade uiFacade) {
    super.init(project, null, uiFacade);
  }

  public void select(List<ResourceSearchService.MySearchResult> results) {
    var selectionManager = getUiFacade().getResourceSelectionManager();
    var selectedResources = results.stream().map(SearchResult::getObject).toList();
    selectionManager.select(selectedResources, true,this);
    getUiFacade().getViewManager().getView(String.valueOf(UIFacade.RESOURCES_INDEX)).setActive(true);
  }
}
