/*
GanttProject is an opensource project management tool.
Copyright (C) 2016 BarD Software s.r.o

This file is part of GanttProject.

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

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;

/**
 * This class shows project opening diagnostics using Swing HTML editor kit. It serves
 * as a fallback on platforms where JavaFX is unavailable.
 *
 * @author dbarashev@bardsoftware.com
 */
class ProjectOpenDiagnosticUiSwing {
  public void run(String msg, ProjectOpenDiagnosticImpl.ShowDialogCallback showDialogCallback) {
    JEditorPane htmlPane = UIUtil.createHtmlPane(msg, NotificationManager.DEFAULT_HYPERLINK_LISTENER);
    htmlPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    showDialogCallback.showDialog(htmlPane);
  }
}
