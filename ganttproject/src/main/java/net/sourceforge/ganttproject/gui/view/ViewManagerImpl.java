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

import biz.ganttproject.app.UninitializedView;
import biz.ganttproject.app.View;
import biz.ganttproject.app.ViewPane;
import javafx.application.Platform;
import javafx.scene.Node;
import kotlin.Unit;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ProjectEventListener;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.edit.CopyAction;
import net.sourceforge.ganttproject.action.edit.CutAction;
import net.sourceforge.ganttproject.action.edit.PasteAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * View manager implementation based on the tab pane.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ViewManagerImpl implements GPViewManager {
  private final Map<ViewProvider, View> myViews = new LinkedHashMap<>();
  private final ViewPane myViewPane;
  //GPView mySelectedView;

  private final CopyAction myCopyAction;
  private final CutAction myCutAction;
  private final PasteAction myPasteAction;
  private final List<ViewProvider> myViewProviders;

  public ViewManagerImpl(IGanttProject project, UIFacade uiFacade, GPUndoManager undoManager, ViewPane viewPane,
                         List<ViewProvider> viewProviders) {
    myViewProviders = viewProviders;
    myViewPane = viewPane;
    project.addProjectEventListener(getProjectEventListener());
    // Create actions
    myCopyAction = new CopyAction(this, uiFacade);
    myCutAction = new CutAction(this, undoManager, uiFacade);
    myPasteAction = new PasteAction(project, uiFacade, this, undoManager);

    /*
    myTabs.getModel().addChangeListener(e -> {
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
    });
     */
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
    return getSelectedView().getChart().getSelection();
  }

  ProjectEventListener getProjectEventListener() {
    return new ProjectEventListener.Stub() {
      @Override
      public void projectClosed() {
        for (ViewProvider view : myViews.keySet()) {
          view.getChart().reset();
        }
      }
    };
  }

  void updateActions() {
    ChartSelection selection = getSelectedView().getChart().getSelection();
    myCopyAction.setEnabled(false == selection.isEmpty());
    myCutAction.setEnabled(false == selection.isEmpty() && selection.isDeletable().isOK());
  }

  @Override
  public Chart getActiveChart() {
    return getSelectedView().getChart();
  }

  @Override
  public void activateNextView() {

  }

  @Override
  public void activatePrevView() {

  }

  public View getSelectedView() {
    return myViews.values().stream().filter(View::isActive).findFirst().orElse(null);
  }

  @Override
  public void createView(ViewProvider viewProvider) {
    Platform.runLater(() -> {
      var fxView = myViewPane.createView(viewProvider);
      myViews.put(viewProvider, fxView);
    });
  }

  @Override
  public Node getFxComponent() {
    return myViewPane.createComponent();
  }

  @Override
  public void onViewCreated(Runnable callback) {
    myViewPane.setOnViewCreated(() -> {callback.run(); return Unit.INSTANCE;});
  }

  @Override
  public void refresh() {
    myViews.values().forEach(View::refresh);
  }

  @Override
  public View getView(String id) {
    var existing = myViews.values().stream().filter(view -> id.equals(view.getId())).findFirst();
    if (existing.isPresent()) {
      return existing.get();
    }
    var provider = myViewProviders.stream().filter(viewProvider -> id.equals(viewProvider.getId())).findFirst();
    if (provider.isPresent()) {
      return new UninitializedView(myViewPane, provider.get());
    } else {
      throw new IllegalStateException("Unknown view with ID=" + id + " available views: " + myViews.keySet().stream().map(p -> p.getId()).collect(Collectors.joining()));
    }
  }
}
