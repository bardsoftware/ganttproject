/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.search;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.plugins.PluginManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class SearchDialog {
  public interface SearchCallback {
    void accept(List<SearchResult<?>> results);
  }

  private final UIFacade myUiFacade;
  private final IGanttProject myProject;

  SearchDialog(IGanttProject project, UIFacade uiFacade) {
    myProject = project;
    myUiFacade = uiFacade;
  }

  void runSearch(final String text, final SearchCallback callback) {
    List<SearchService> services = PluginManager.getExtensions(SearchService.EXTENSION_POINT_ID, SearchService.class);
    final List<Future<List<SearchResult<?>>>> tasks = new ArrayList<Future<List<SearchResult<?>>>>();
    ExecutorService executor = Executors.newFixedThreadPool(services.size());
    for (final SearchService<SearchResult<?>, ?> service : services) {
      service.init(myProject, myUiFacade);
      tasks.add(executor.submit(new Callable<List<SearchResult<?>>>() {
        @Override
        public List<SearchResult<?>> call() throws Exception {
          List<SearchResult<?>> search = service.search(text);
          return search;
        }
      }));
    }
    SwingWorker<List<SearchResult<?>>, Object> worker = new SwingWorker<List<SearchResult<?>>, Object>() {
      @Override
      protected List<SearchResult<?>> doInBackground() throws Exception {
        List<SearchResult<?>> totalResult = new ArrayList<SearchResult<?>>();
        for (Future<List<SearchResult<?>>> f : tasks) {
          totalResult.addAll(f.get());
        }
        return totalResult;
      }

      @Override
      protected void done() {
        try {
          callback.accept(get());
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
    };
    worker.execute();
  }
}
