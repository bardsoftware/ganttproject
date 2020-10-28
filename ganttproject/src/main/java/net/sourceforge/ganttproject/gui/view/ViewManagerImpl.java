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

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ProjectEventListener;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.edit.CopyAction;
import net.sourceforge.ganttproject.action.edit.CutAction;
import net.sourceforge.ganttproject.action.edit.PasteAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.gui.GanttTabbedPane;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * View manager implementation based on the tab pane.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ViewManagerImpl implements GPViewManager {
  private final GanttTabbedPane myTabs;
  private final Map<GPView, ViewHolder> myViews = new LinkedHashMap<GPView, ViewHolder>();
  GPView mySelectedView;

  private final CopyAction myCopyAction;
  private final CutAction myCutAction;
  private final PasteAction myPasteAction;

  public ViewManagerImpl(IGanttProject project, UIFacade uiFacade, GanttTabbedPane tabs, GPUndoManager undoManager) {
    myTabs = tabs;
    project.addProjectEventListener(getProjectEventListener());
    // Create actions
    myCopyAction = new CopyAction(this);
    myCutAction = new CutAction(this, undoManager);
    myPasteAction = new PasteAction(project, uiFacade, this, undoManager);

    myTabs.addChangeListener(new ChangeListener() {

      @Override
      public void stateChanged(ChangeEvent e) {
        GPView selectedView = (GPView) myTabs.getSelectedUserObject();
        if (mySelectedView == selectedView) {
          return;
        }
        if (mySelectedView != null) {
          mySelectedView.setActive(false);
          myViews.get(mySelectedView).setActive(false);
        }
        mySelectedView = selectedView;
        mySelectedView.setActive(true);
        myViews.get(mySelectedView).setActive(true);
        updateActions();
      }
    });
  }

  @Override
  public GPAction getCopyAction() {
    return myCopyAction;
  }

  @Override
  public GPAction getCutAction() {
    return myCutAction;
  }

  @Override
  public GPAction getPasteAction() {
    return myPasteAction;
  }

  @Override
  public ChartSelection getSelectedArtefacts() {
    return mySelectedView.getChart().getSelection();
  }

  ProjectEventListener getProjectEventListener() {
    return new ProjectEventListener.Stub() {
      @Override
      public void projectClosed() {
        for (GPView view : myViews.keySet()) {
          view.getChart().reset();
        }
      }
    };
  }

  void updateActions() {
    ChartSelection selection = mySelectedView.getChart().getSelection();
    myCopyAction.setEnabled(false == selection.isEmpty());
    myCutAction.setEnabled(false == selection.isEmpty() && selection.isDeletable().isOK());
  }

  @Override
  public Chart getActiveChart() {
    return mySelectedView.getChart();
  }

  @Override
  public void activateNextView() {
    myTabs.setSelectedIndex((myTabs.getSelectedIndex() + 1) % myTabs.getTabCount());
  }

  @Override
  public void activatePrevView() {
    myTabs.setSelectedIndex((myTabs.getSelectedIndex() - 1 + myTabs.getTabCount()) % myTabs.getTabCount());
  }

  public GPView getSelectedView() {
    return mySelectedView;
  }

  @Override
  public void createView(GPView view, Icon icon) {
    ViewHolder viewHolder = new ViewHolder(this, myTabs, view, icon);
    myViews.put(view, viewHolder);
  }

  @Override
  public void toggleVisible(GPView view) {
    ViewHolder viewHolder = myViews.get(view);
    assert viewHolder != null;
    viewHolder.setVisible(!viewHolder.isVisible());
  }
}
