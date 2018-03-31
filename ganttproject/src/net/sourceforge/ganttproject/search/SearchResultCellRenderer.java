package net.sourceforge.ganttproject.search;

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

import net.sourceforge.ganttproject.language.GanttLanguage;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class SearchResultCellRenderer implements ListCellRenderer {

    private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

    @Override
    public Component getListCellRendererComponent(JList jList, Object o, int index, boolean b, boolean b1) {
        StringBuilder theText = new StringBuilder();
        theText.append("<html>");

        JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(jList, o, index,
                b, b1);

        if (o instanceof SearchResult) {
            SearchResult searchResult = (SearchResult) o;
            String label = searchResult.getLabel();
            String searchTerm = searchResult.getQueryMatch();
            String boldedLabel = label.replaceAll("(?i)" + searchTerm + "", "<b><i>$0</i></b>");
            theText.append(GanttLanguage.getInstance().formatText("search.result.line1", searchResult.getTypeOfResult(), searchResult.getMyId(), boldedLabel));
            if (!searchResult.getSecondaryLabel().isEmpty()) {
                String secondaryText = searchResult.getSecondaryText();
                String boldedSecondaryText = secondaryText.replaceAll("(?i)" + searchTerm + "", "<b><i>$0</i></b>");
                theText.append(GanttLanguage.getInstance().formatText("search.result.line2", searchResult.getSecondaryLabel(), boldedSecondaryText));
            }
        } else {
            theText.append(o.toString());
        }
        theText.append("</html>");
        renderer.setText(theText.toString());
        renderer.setFont(renderer.getFont().deriveFont(Font.PLAIN));
        return renderer;
    }
}
