/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 Benoit Baranne, GanttProject Team

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
package net.sourceforge.ganttproject.task;

/**
 * Exception to be thrown in several cases : A custom column already exists (and
 * someone tries to add a nex one with the same name) A custom column does not
 * exists (and someone tried to get it) A class mismatch.
 * 
 * @author bbaranne (Benoit Baranne)
 */
public class CustomColumnsException extends Exception {
  public static final int ALREADY_EXIST = 0;

  public static final int DO_NOT_EXIST = 1;

  public static final int CLASS_MISMATCH = 2;

  /**
   * Exception type.
   */
  private int type = -1;

  public CustomColumnsException(int type, String message) {
    super(message);
    this.type = type;
  }

  public CustomColumnsException(Throwable cause) {
    super(cause);
  }

  public int getType() {
    return type;
  }
}
