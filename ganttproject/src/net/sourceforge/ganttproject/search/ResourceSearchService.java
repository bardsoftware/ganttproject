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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.HumanResource;

/** Search service for resources */
public class ResourceSearchService extends SearchServiceBase<ResourceSearchService.MySearchResult, HumanResource> {
  static class MySearchResult extends SearchResult<HumanResource> {
    public MySearchResult(HumanResource hr, ResourceSearchService searchService) {
      super("Resource: " + hr.getName(), "", "", hr, searchService);
    }
  }

  public ResourceSearchService() {
    super(UIFacade.RESOURCES_INDEX);
  }

  @Override
  public List<MySearchResult> search(String query) {
    query = query.toLowerCase();
    List<MySearchResult> results = new ArrayList<MySearchResult>();
    for (HumanResource hr : getProject().getHumanResourceManager().getResources()) {
      if (isNotEmptyAndContains(hr.getName(), query)) {
        results.add(new MySearchResult(hr, this));
      }
    }
    return results;
  }

  @Override
  public void init(IGanttProject project, UIFacade uiFacade) {
    super.init(project, uiFacade.getResourceTree(), uiFacade);
  }
}
