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

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;

import javax.swing.*;
import java.awt.*;

public class SearchUiImpl implements SearchUi {
  private final IGanttProject project;
  private final UIFacade uiFacade;
  private JTextField searchBox;
  private Color myInitialForeground;

  public SearchUiImpl(IGanttProject project, UIFacade uiFacade) {
    this.project = project;
    this.uiFacade = uiFacade;
//    PopupSearchCallback callback = new PopupSearchCallback();
//    searchBox = new JTextField(30);
//    myInitialForeground = searchBox.getForeground();
//    searchBox.setForeground(Color.GRAY);
//    final GanttLanguage i18n = GanttLanguage.getInstance();
//    i18n.addListener(new GanttLanguage.Listener() {
//      @Override
//      public void languageChanged(Event event) {
//        searchBox.setText(i18n.formatText("search.prompt.pattern", i18n.getText("search.dialog.search"),
//            i18n.getText("search.prompt.shortcut")));
//      }
//    });
    //callback.setSearchBox(searchBox);
  }

  @Override
  public void requestFocus() {
    searchBox.requestFocusInWindow();
  }

  public JTextField getSearchField() {
    return searchBox;
  }

}
