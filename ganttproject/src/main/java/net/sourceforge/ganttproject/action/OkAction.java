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

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import java.awt.event.ActionEvent;

/**
 * Default OK/confirm action for dialogs.
 * adds additional/special functionalities for this action
 */
public abstract class OkAction extends GPAction {
  private boolean isDefault = true;

  public OkAction() {
    this("ok");
  }

  public OkAction(String key) {
    super(key);
  }

  public boolean isDefault() {
    return isDefault;
  }

  protected void setDefault(boolean value) {
    isDefault = value;
  }

  public static OkAction create(String key, Function0<Unit> handler) {
    return new OkAction(key) {
      @Override
      public void actionPerformed(ActionEvent e) {
        handler.invoke();
      }
    };
  }
}
