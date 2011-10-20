package net.sourceforge.ganttproject.search;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;

public class SimpleSearchService implements SearchService {

    class MySearchResult extends SearchResult {
        private final Task myTask;
        public MySearchResult(Task t) {
            super(t.getName(), "", "", SimpleSearchService.this);
            myTask = t;
        }

        Task getTask() {
            return myTask;
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
        for (Task t : myProject.getTaskManager().getTasks()) {
            if (isNotEmptyAndContains(t.getName(), query) || isNotEmptyAndContains(t.getNotes(), query)) {
                results.add(new MySearchResult(t));
            }
        }
        return results;
    }

    @Override
    public void select(List<SearchResult> results) {
        myUiFacade.getTaskSelectionManager().clear();
        for (SearchResult r : results) {
            MySearchResult result = (MySearchResult) r;
            myUiFacade.getTaskSelectionManager().addTask(result.getTask());
            myUiFacade.getTaskTree().getTreeComponent().requestFocusInWindow();
        }
    }

}
