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
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

/**
 * Default cancel action for dialogs.
 * {@link UIFacade#createDialog(java.awt.Component, javax.swing.Action[], String)}
 * adds additional/special functionalities for this action
 */
public class CancelAction extends GPAction {
  /** CancelAction which does not do anything */
  public final static CancelAction EMPTY = new CancelAction();

  public static final CancelAction CLOSE = new CancelAction("close");

  public CancelAction() {
    this("cancel");
  }

  /** For concealed CancelActions (with a different text) */
  public CancelAction(String key) {
    super(key);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // Do nothing, as cancel mostly does nothing
  }
}
