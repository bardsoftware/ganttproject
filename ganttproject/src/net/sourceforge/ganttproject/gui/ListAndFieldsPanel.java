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
package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

public class ListAndFieldsPanel<T> {
  private EditableList<T> myList;
  private JComponent myFields;
  private Box myPanel;

  public ListAndFieldsPanel(EditableList<T> list, JComponent fields) {
    myList = list;
    myFields = fields;
  }

  public JComponent getComponent() {
    if (myPanel == null) {
      SpringLayout topPanelLayout = new SpringLayout();
      JPanel topPanel = new JPanel(topPanelLayout);

      JComponent depsComponent = myList.getTableComponent();
      JComponent titleComponent = new JLabel(myList.getTitle());
      JComponent actionsComponent = myList.getActionsComponent();
      topPanel.add(titleComponent);
      topPanel.add(actionsComponent);

      topPanelLayout.putConstraint(SpringLayout.WEST, titleComponent, 0, SpringLayout.WEST, topPanel);
      topPanelLayout.putConstraint(SpringLayout.NORTH, titleComponent, 0, SpringLayout.NORTH, topPanel);

      topPanelLayout.putConstraint(SpringLayout.NORTH, actionsComponent, 2, SpringLayout.SOUTH, titleComponent);
      topPanelLayout.putConstraint(SpringLayout.SOUTH, topPanel, 2, SpringLayout.SOUTH, actionsComponent);
      topPanelLayout.putConstraint(SpringLayout.WEST, actionsComponent, 0, SpringLayout.WEST, topPanel);

      JPanel centerPanel = new JPanel(new BorderLayout());
      centerPanel.add(depsComponent, BorderLayout.CENTER);

      myFields.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
      centerPanel.add(myFields, BorderLayout.EAST);

      myPanel = Box.createVerticalBox();
      myPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      myPanel.add(topPanel);
      myPanel.add(Box.createVerticalStrut(5));
      myPanel.add(centerPanel);
    }
    return myPanel;
  }
}
