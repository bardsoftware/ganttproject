/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.action.resource;

import net.sourceforge.ganttproject.action.ActionDelegate;
import net.sourceforge.ganttproject.action.ActionStateChangedListener;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceContext;

import java.util.ArrayList;
import java.util.List;

//TODO Add listener for changed resource selection, see TaskActionBase
/**
 * Action base for resource related actions
 */
abstract class ResourceAction extends GPAction implements ActionDelegate {
  private final HumanResourceManager myManager;
  private final List<ActionStateChangedListener> myListeners = new ArrayList<ActionStateChangedListener>();
  private final ResourceContext myContext;

  public ResourceAction(String name, HumanResourceManager hrManager) {
    this(name, hrManager, null, IconSize.NO_ICON);
  }

  protected ResourceAction(String name, HumanResourceManager hrManager, ResourceContext context, IconSize size) {
    super(name, size.asString());
    myManager = hrManager;
    myContext = context;
  }

  @Override
  public void addStateChangedListener(ActionStateChangedListener l) {
    myListeners.add(l);
  }

  protected HumanResourceManager getManager() {
    return myManager;
  }

  protected ResourceContext getContext() {
    return myContext;
  }

  protected boolean hasResources() {
    HumanResource[] selection = myContext.getResources();
    return selection != null && selection.length > 0;
  }

  protected HumanResource[] getSelection() {
    HumanResource[] selection = myContext.getResources();
    return selection == null ? new HumanResource[0] : selection;
  }

  @Override
  public void setEnabled(boolean newValue) {
    super.setEnabled(newValue);
    for (ActionStateChangedListener l : myListeners) {
      l.actionStateChanged();
    }
  }
}
