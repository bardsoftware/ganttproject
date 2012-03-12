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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.plugins.PluginManager;

public class SearchDialog {
  public interface SearchCallback {
    void accept(List<SearchResult<?>> results);
  }

  private final UIFacade myUiFacade;
  private DefaultListModel myResultViewDataModel;
  private final IGanttProject myProject;
  private JList myResultView;
  private UIFacade.Dialog myDialog;
  private SearchCallback mySearchCallback;

  public SearchDialog(IGanttProject project, UIFacade uiFacade) {
    myProject = project;
    myUiFacade = uiFacade;
    myResultViewDataModel = new DefaultListModel();
    myDialog = myUiFacade.createDialog(getComponent(), new Action[] { new CancelAction("search.dialog.goto") {
      @Override
      public void actionPerformed(ActionEvent e) {
        gotoSelection();
      }
    }, new CancelAction("close") }, GanttLanguage.getInstance().getText("search.dialog.title"));
    mySearchCallback = new SearchCallback() {
      @Override
      public void accept(List<SearchResult<?>> results) {
        processResults(results);
      }
    };
  }

  public void show() {
    myDialog.show();
  }

  protected void gotoSelection() {
    SearchResult selectedValue = (SearchResult) myResultView.getSelectedValue();
    if (selectedValue != null) {
      selectedValue.getSearchService().select(Collections.singletonList(selectedValue));
    }
  }

  public JTextField createSearchBox(final SearchCallback callback) {
    final JTextField inputField = new JTextField(30);
    inputField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          runSearch(inputField.getText(), callback);
        }
      }
    });
    return inputField;
  }

  private JComponent getComponent() {
    JPanel result = new JPanel(new BorderLayout());
    JPanel inputPanel = new JPanel(new BorderLayout());
    final JTextField inputField = new JTextField(30);
    final GPAction searchAction = new GPAction("search.dialog.search") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        runSearch(inputField.getText(), mySearchCallback);
      }
    };
    JButton searchButton = new JButton(searchAction);
    // TODO Add keyboard shortcut to searchAction
    inputField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          searchAction.actionPerformed(null);
        }
      }
    });
    inputPanel.add(inputField, BorderLayout.CENTER);
    inputPanel.add(searchButton, BorderLayout.EAST);
    inputPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
    result.add(inputPanel, BorderLayout.NORTH);
    myResultView = new JList(getResultViewDataModel());
    myResultView.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          gotoSelection();
          myDialog.hide();
        }
      }
    });
    result.add(new JScrollPane(myResultView), BorderLayout.CENTER);
    result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    return result;
  }

  private ListModel getResultViewDataModel() {
    return myResultViewDataModel;
  }

  void runSearch(final String text, final SearchCallback callback) {
    myResultViewDataModel.clear();
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
          processResults(get());
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }
    };
    worker.execute();
  }

  protected void processResults(final List<SearchResult<?>> results) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        for (SearchResult<?> r : results) {
          myResultViewDataModel.addElement(r);
        }
      }
    });
  }
}
