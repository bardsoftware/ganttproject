/*
Copyright 2018 BarD Software s.r.o, Douglas Kelly

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

import net.sourceforge.ganttproject.language.GanttLanguage;

import java.awt.Component;
import java.awt.Font;

import javax.swing.*;

public class SearchResultCellRenderer implements ListCellRenderer {

  private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

  @Override
  public Component getListCellRendererComponent(JList jList, Object searchResultObject, int index, boolean isSelected, boolean cellHasFocus) {
    StringBuilder theText = new StringBuilder();
    theText.append("<html>");

    JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(jList, searchResultObject, index,
        isSelected, cellHasFocus);

    if (searchResultObject instanceof SearchResult) {
      SearchResult searchResult = (SearchResult) searchResultObject;
      String label = searchResult.getLabel();
      String searchTerm = searchResult.getQueryMatch();
      String boldedLabel = label.replaceAll("(?i)" + searchTerm, "<b><i>$0</i></b>");
      theText.append(GanttLanguage.getInstance().formatText("search.result.line1", searchResult.getTypeOfResult(), searchResult.getId(), boldedLabel));
      if (!searchResult.getSecondaryLabel().isEmpty()) {
        String secondaryText = searchResult.getSecondaryText();
        String boldedSecondaryText = secondaryText.replaceAll("(?i)" + searchTerm, "<b><i>$0</i></b>");
        theText.append(GanttLanguage.getInstance().formatText("search.result.line2", searchResult.getSecondaryLabel(), boldedSecondaryText));
      }
    } else {
      theText.append(searchResultObject.toString());
    }
    theText.append("</html>");
    renderer.setText(theText.toString());
    renderer.setBorder(BorderFactory.createEmptyBorder(5, 3, 5, 3));
    renderer.setFont(renderer.getFont().deriveFont(Font.PLAIN));
    return renderer;
  }
}
