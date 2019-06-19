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

import com.bardsoftware.eclipsito.update.UpdateMetadata;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.eclipse.core.runtime.Platform;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class UpdateDialog {
  public static void show(UIFacade uiFacade, UpdateMetadata update) {
    JEditorPane versionRow = createHtml(GanttLanguage.getInstance().formatText("updateAvailable", update.version));
    JEditorPane descriptionRow = createHtml(update.description);

    Box dialogBox = Box.createVerticalBox();
    dialogBox.add(versionRow);
    dialogBox.add(descriptionRow);

    JPanel panel = new JPanel();
    panel.add(dialogBox);

    SwingUtilities.invokeLater(() -> {
      OkAction okAction = new OkAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            Platform.getUpdater().installUpdate(update, percents -> System.out.println(String.format("Downloading... %d%% done", percents)))
                .thenAccept(file -> System.out.println("Installed into " + file))
                .exceptionally(ex -> {
                  GPLogger.log(ex);
                  return null;
                });
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      };

      UIFacade.Dialog dlg = uiFacade.createDialog(panel, new Action[]{okAction, CancelAction.EMPTY}, "");
      dlg.show();

    });
  }

  private static JEditorPane createHtml(String html) {
    JEditorPane htmlPane = new JEditorPane("text/html", html);
    htmlPane.setEditable(false);
    htmlPane.setBackground(new JPanel().getBackground());
    htmlPane.addHyperlinkListener(NotificationManager.DEFAULT_HYPERLINK_LISTENER);
    return htmlPane;
  }

}
