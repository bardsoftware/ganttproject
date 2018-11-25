/*
Copyright 2018 BarD Software s.r.o

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
import net.sourceforge.ganttproject.gui.UIUtil;
import org.jdesktop.swingx.JXList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

/**
 * @author dbarashev@bardsoftware.com
 */
public class PopupSearchCallback implements SearchDialog.SearchCallback {
  private final Rectangle mySearchBoxPosition;
  private final JComponent myInvoker;
  private SearchDialog myDialog;
  private JXList list = new JXList();
  private Runnable onSelect;
  private Runnable onDismiss;

  public PopupSearchCallback(IGanttProject project, UIFacade uiFacade, JComponent invoker, Rectangle searchBoxPosition) {
    myDialog = new SearchDialog(project, uiFacade);
    myInvoker = invoker;
    mySearchBoxPosition = searchBoxPosition;
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
    SearchResult[] searchResults = results.toArray(new SearchResult[0]);
    list.setListData(searchResults);
    int searchResultLength = searchResults.length;
    if (searchResultLength < 9) {
      list.setVisibleRowCount(searchResultLength);
    } else {
      list.setVisibleRowCount(10);
    }
    JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    final JPopupMenu popup = new JPopupMenu();
    popup.add(scrollPane);
    popup.setPopupSize(mySearchBoxPosition.width, 300);
    popup.show(myInvoker, mySearchBoxPosition.x, mySearchBoxPosition.y + mySearchBoxPosition.height);
    list.requestFocusInWindow();
    ListCellRenderer resultRenderer = new SearchResultCellRenderer();
    list.setCellRenderer(resultRenderer);
    list.setHighlighters(UIUtil.ZEBRA_HIGHLIGHTER);
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
      }
    };
  }

  private void onSelect(JPopupMenu popup, List<SearchResult<?>> results) {
    popup.setVisible(false);
    SearchResult selectedValue = results.get(list.getSelectedIndex());
    selectedValue.getSearchService().select(Collections.singletonList(selectedValue));
  }

  public void runSearch(String query) {
    myDialog.runSearch(query, this);
  }
}
