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

import biz.ganttproject.app.DialogKt;
import biz.ganttproject.app.InternationalizationKt;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import kotlin.Unit;
import net.sourceforge.ganttproject.GPLogger;

/**
 * Small utility to show logs in a dialog.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class ViewLogDialog {
  public static void show() {
    DialogKt.dialog(InternationalizationKt.getRootLocalizer().formatText("viewLog"), "viewLog", dlg -> {
      dlg.addStyleSheet("/biz/ganttproject/app/Dialog.css");
      dlg.addStyleClass("dlg");
      var textArea = new TextArea(GPLogger.readLog());
      textArea.setPrefColumnCount(40);
      textArea.setPrefRowCount(40);
      dlg.setContent(textArea);
      dlg.setOnShown(() -> {
        dlg.resize();
        return Unit.INSTANCE;
      });
      dlg.setupButton(ButtonType.CLOSE, btn -> {
        btn.getStyleClass().addAll("btn", "btn-attention");
        btn.setText(InternationalizationKt.getRootLocalizer().formatText("close"));
        btn.setOnAction(event -> {
          dlg.hide();
        });
        return Unit.INSTANCE;
      });
      return Unit.INSTANCE;
    });
//    Dialog dlg = uiFacade.createDialog(scrollPane, new Action[] { CancelAction.CLOSE }, "");
//    dlg.show();

  }
}
