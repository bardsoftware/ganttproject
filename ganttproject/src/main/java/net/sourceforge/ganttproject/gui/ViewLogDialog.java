/*
GanttProject is an opensource project management tool. License: GPL3
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
package net.sourceforge.ganttproject.gui;

import javax.swing.Action;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.gui.UIFacade.Dialog;

/**
 * Small utility to show logs in a dialog.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class ViewLogDialog {
  public static void show(UIFacade uiFacade) {
    JTextArea textArea = new JTextArea(GPLogger.readLog(), 20, 80);
    JScrollPane scrollPane = new JScrollPane(textArea);
    Dialog dlg = uiFacade.createDialog(scrollPane, new Action[] { CancelAction.CLOSE }, "");
    dlg.show();

  }
}
