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
import org.apache.commons.lang.SystemUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;

public class RestartDialog {

  public static void show(UIFacade uiFacade) {
    JLabel label = new JLabel(GanttLanguage.getInstance().formatText("restartApplication"));

    JPanel panel = new JPanel();
    panel.add(label);

    OkAction okAction = new OkAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ScheduledExecutorService schedulerExecutor = Executors.newScheduledThreadPool(2);
        Callable<Process> callable = () -> {
          ProcessBuilder builder = new ProcessBuilder(
                  SystemUtils.IS_OS_WINDOWS? "cmd /c start \"\" ganttproject.bat" : "ganttproject");

          return builder.start();
        };
        FutureTask<Process> futureTask = new FutureTask<>(callable);
        schedulerExecutor.submit(futureTask);

        System.exit(0);
      }
    };

    UIFacade.Dialog dlg = uiFacade.createDialog(panel, new Action[]{okAction, CancelAction.EMPTY}, "");
    dlg.show();
  }
}
