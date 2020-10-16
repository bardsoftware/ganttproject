/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.gui;

import biz.ganttproject.core.table.ColumnList;
import com.google.common.base.Predicate;
import net.sourceforge.ganttproject.action.GPAction;

import javax.swing.*;
import java.awt.*;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public interface TreeUiFacade<T> {
  Component getTreeComponent();

  ColumnList getVisibleFields();

  boolean isVisible(T modelElement);

  boolean isExpanded(T modelElement);

  void setExpanded(T modelElement, boolean value);

  void applyPreservingExpansionState(T modelElement, Predicate<T> callable);
  /**
   * Modifies the selected node(s) of the tree
   *
   * @param clear
   *          when true, it first clears the previous selection. When false the
   *          current selection gets extended
   * @param modelElement
   *          to be selected
   */
  void setSelected(T modelElement, boolean clear);

  /** Clears the current selection */
  void clearSelection();

  void makeVisible(T modelElement);

  GPAction getNewAction();

  GPAction getPropertiesAction();

  GPAction getDeleteAction();

  void startDefaultEditing(T modelElement);
  void stopEditing();
  AbstractAction[] getTreeActions();
}
