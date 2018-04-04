/*
Copyright 2018 Oleksii Lapinskyi, BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

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
package net.sourceforge.ganttproject.gui.update;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import static javax.swing.JOptionPane.QUESTION_MESSAGE;

public class RestartDialog {

  public static void show(UIFacade uiFacade) {

    OkAction okAction = new OkAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        uiFacade.getMainFrame().dispatchEvent(new WindowEvent(uiFacade.getMainFrame(), WindowEvent.WINDOW_CLOSING));
      }
    };
    uiFacade.showOptionDialog(QUESTION_MESSAGE, GanttLanguage.getInstance().formatText("restartApplication"), new Action[]{okAction, CancelAction.EMPTY});

  }
}
