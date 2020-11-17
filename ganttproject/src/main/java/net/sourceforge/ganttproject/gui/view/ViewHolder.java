/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.gui.view;

import javax.swing.Icon;

import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.gui.GanttTabbedPane;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;

/**
 * Controller which hides/shows view tab.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
class ViewHolder implements ChartSelectionListener, GanttLanguage.Listener {
  private final GanttTabbedPane myTabs;

  private int myIndex;

  private boolean isVisible;

  private final Icon myIcon;

  private final ViewManagerImpl myManager;

  private final GPView myView;

  ViewHolder(ViewManagerImpl manager, GanttTabbedPane tabs, GPView view, Icon icon) {
    myManager = manager;
    myTabs = tabs;
    myView = view;
    myIcon = icon;
    GanttLanguage.getInstance().addListener(this);
    assert myView != null;
  }

  void setActive(boolean active) {
    if (active) {
      myView.getChart().addSelectionListener(this);
    } else {
      myView.getChart().removeSelectionListener(this);
    }
  }

  void setVisible(boolean isVisible) {
    if (isVisible) {
      String tabName = myView.getChart().getName();
      myTabs.addTab(tabName, myIcon, myView.getViewComponent(), tabName, myView);
      myTabs.setSelectedComponent(myView.getViewComponent());
      myIndex = myTabs.getSelectedIndex();

    } else {
      myTabs.remove(myIndex);
    }
    this.isVisible = isVisible;
  }

  boolean isVisible() {
    return isVisible;
  }

  @Override
  public void selectionChanged() {
    myManager.updateActions();
  }

  @Override
  public void languageChanged(Event event) {
    if (isVisible()) {
      myTabs.setTitleAt(myIndex, myView.getChart().getName());
    }
  }
}