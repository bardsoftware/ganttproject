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
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.HumanResource;

/** Search service for resources */
public class ResourceSearchService implements SearchService {

    class MySearchResult extends SearchResult {
        private final HumanResource myResource;

        public MySearchResult(HumanResource r) {
            super("Resource: " + r.getName(), "", "", ResourceSearchService.this);
            myResource = r;
        }

        public HumanResource getResource() {
            return myResource;
        }

    }

    private IGanttProject myProject;
    private UIFacade myUiFacade;

    @Override
    public void init(IGanttProject project, UIFacade uiFacade) {
        myProject = project;
        myUiFacade = uiFacade;
    }

    private static boolean isNotEmptyAndContains(String doc, String query) {
        return doc != null && doc.toLowerCase().contains(query);
    }

    @Override
    public List<SearchResult> search(String query) {
        query = query.toLowerCase();
        List<SearchResult> results = new ArrayList<SearchResult>();
        for (HumanResource r : myProject.getHumanResourceManager().getResources()) {
            if (isNotEmptyAndContains(r.getName(), query)) {
                results.add(new MySearchResult(r));
            }
        }
        return results;
    }

    @Override
    public void select(List<SearchResult> results) {
        ResourceTreeUIFacade resourceTree = myUiFacade.getResourceTree();
        resourceTree.clearSelection();
        for (SearchResult r : results) {
            MySearchResult result = (MySearchResult) r;
            resourceTree.setSelected(result.getResource(), false);
        }
        resourceTree.getTreeComponent().requestFocusInWindow();
    }
}
