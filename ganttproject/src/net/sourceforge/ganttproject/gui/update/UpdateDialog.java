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

import net.sourceforge.ganttproject.DownloadWorker;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.client.RssUpdate;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class UpdateDialog {
  public static void show(UIFacade uiFacade, RssUpdate update) {
    JEditorPane versionRow = createHtml(GanttLanguage.getInstance().formatText("updateAvailable", update.getVersion()));
    JEditorPane descriptionRow = createHtml(update.getDescription());

    Box dialogBox = Box.createVerticalBox();
    dialogBox.add(versionRow);
    dialogBox.add(descriptionRow);

    JPanel panel = new JPanel();
    panel.add(dialogBox);

    SwingUtilities.invokeLater(() -> {
      OkAction okAction = new OkAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          new DownloadWorker(uiFacade, update.getUrl()).execute();
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
