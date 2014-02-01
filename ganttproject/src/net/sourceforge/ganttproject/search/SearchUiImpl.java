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

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.search.SearchDialog.SearchCallback;

public class SearchUiImpl implements SearchUi {
  private final IGanttProject project;
  private final UIFacade uiFacade;
  private JTextField searchBox;
  private Color myInitialForeground;

  public SearchUiImpl(IGanttProject project, UIFacade uiFacade) {
    this.project = project;
    this.uiFacade = uiFacade;
    PopupSearchCallback callback = new PopupSearchCallback();
    searchBox = new JTextField(30);
    myInitialForeground = searchBox.getForeground();
    searchBox.setForeground(Color.GRAY);
    final GanttLanguage i18n = GanttLanguage.getInstance();
    i18n.addListener(new GanttLanguage.Listener() {
      @Override
      public void languageChanged(Event event) {
        searchBox.setText(i18n.formatText("search.prompt.pattern", i18n.getText("search.dialog.search"),
            i18n.getText("search.prompt.shortcut")));
      }
    });
    callback.setSearchBox(searchBox);
  }

  @Override
  public void requestFocus() {
    searchBox.requestFocusInWindow();
  }

  public JTextField getSearchField() {
    return searchBox;
  }

  class PopupSearchCallback implements SearchCallback {
    private SearchDialog myDialog = new SearchDialog(project, uiFacade);
    private JTextField searchBox;
    private JList list = new JList();
    private Runnable onSelect;
    private Runnable onDismiss;

    public PopupSearchCallback() {
      list.setBorder(BorderFactory.createEmptyBorder());
      list.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          switch (e.getKeyCode()) {
          case KeyEvent.VK_ENTER:
            e.consume();
            e.setKeyCode(0);
            onSelect.run();
            break;
          case KeyEvent.VK_ESCAPE:
            onDismiss.run();
            break;
          }
        }
      });
      list.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          onSelect.run();
        }
      });
    }

    @Override
    public void accept(final List<SearchResult<?>> results) {
      if (results.isEmpty()) {
        return;
      }
      list.setListData(results.toArray(new SearchResult[0]));
      JScrollPane scrollPane = new JScrollPane(list);
      scrollPane.setBorder(BorderFactory.createEmptyBorder());
      final JPopupMenu popup = new JPopupMenu();
      popup.add(scrollPane);
      popup.show(searchBox, 0, searchBox.getHeight());
      list.requestFocusInWindow();
      list.setSelectedIndex(0);
      onSelect = new Runnable() {
        @Override
        public void run() {
          onSelect(popup, results);
        }
      };
      onDismiss = new Runnable() {
        @Override
        public void run() {
          popup.setVisible(false);
          searchBox.requestFocusInWindow();
        }
      };
    }

    private void onSelect(JPopupMenu popup, List<SearchResult<?>> results) {
      popup.setVisible(false);
      SearchResult selectedValue = results.get(list.getSelectedIndex());
      selectedValue.getSearchService().select(Collections.singletonList(selectedValue));
    }

    void setSearchBox(JTextField searchBox) {
      this.searchBox = searchBox;
      searchBox.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ENTER && PopupSearchCallback.this.searchBox.isFocusOwner()) {
            runSearch();
          }
        }
      });
      this.searchBox.addFocusListener(new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
          PopupSearchCallback.this.searchBox.selectAll();
          PopupSearchCallback.this.searchBox.setForeground(myInitialForeground);
        }
      });
    }

    void dismissPopup() {

    }

    protected void runSearch() {
      myDialog.runSearch(searchBox.getText(), this);
    }
  }
}
