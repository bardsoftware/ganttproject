/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2016 GanttProject team

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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmBase;
import net.sourceforge.ganttproject.util.collect.Pair;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import biz.ganttproject.core.time.CalendarFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Diagnostic class collects information from the scheduler when user opens
 * a project. Should we have any changes in the task dates after the first scheduler
 * run, we report them to the user.
 *
 * @author dbarashev@ganttproject.biz (Dmitry Barashev)
 */
class ProjectOpenDiagnosticImpl implements AlgorithmBase.Diagnostic {
  private final UIFacade myUiFacade;

  List<String> myMessages = Lists.newArrayList();
  LinkedHashMap<Task, Pair<Date, Date>> myModifiedTasks = new LinkedHashMap<>();
  Map<Task, String> myReasons = Maps.newHashMap();
  private boolean myHasOnlyEndDateChange = false;
  private GanttLanguage i18n = GanttLanguage.getInstance();

  ProjectOpenDiagnosticImpl(UIFacade uiFacade) {
    myUiFacade = Preconditions.checkNotNull(uiFacade);
  }

  void info(String message) {
    myMessages.add(message);
  }
  @Override
  public void addModifiedTask(Task t, Date newStart, Date newEnd) {
    Pair<Date, Date> entry = myModifiedTasks.get(t);
    if (entry == null) {
      entry = Pair.create(null, null);
    }
    if (newStart != null) {
      entry = Pair.create(newStart, entry.second());
    }
    if (newEnd != null) {
      entry = Pair.create(entry.first(), newEnd);
    }
    if (entry.first() == null && entry.second() != null) {
      myHasOnlyEndDateChange = true;
    }
    myModifiedTasks.put(t, entry);
  }
  void addReason(Task t, String reasonKey) {
    myReasons.put(t, reasonKey);
  }
  void showDialog() {
    myMessages.add(0,  "");
    String intro = Joiner.on("<li>").join(myMessages);
    String startDateChangeTable = buildStartDateChangeTable();
    String endDateChangeTable = myHasOnlyEndDateChange ? buildEndDateChangeTable() : "";
    //String reasonTable = buildReasonTable();
    final String msg = String.format(i18n.getText("scheduler.warning.template"),
        i18n.getText("scheduler.warning.h1"),
        i18n.getText("scheduler.warning.intro1"),
        intro,
        i18n.getText("scheduler.warning.intro2"),
        i18n.getText("scheduler.warning.details.url"),
        i18n.getText("updateRss.question.2"),
        startDateChangeTable,
        endDateChangeTable
    );

    final JFXPanel contentPane = new JFXPanel();
    final Runnable showDialog = new Runnable() {
      public void run() {
        myUiFacade.createDialog(contentPane, new Action[] {CancelAction.CLOSE}, "").show();
      }
    };
    Platform.runLater(new Runnable() {
      public void run() {
        VBox root = new VBox();
        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(browser);
        webEngine.loadContent(msg);

        setOpenLinksInBrowser(webEngine);

        root.getChildren().addAll(scrollPane);
        Scene scene = new Scene(new Group());
        scene.setRoot(root);
        contentPane.setScene(scene);
        SwingUtilities.invokeLater(showDialog);
      }
    });
  }

  private static void setOpenLinksInBrowser(final WebEngine webEngine) {
    webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
      public void changed(
          ObservableValue<? extends javafx.concurrent.Worker.State> observable,
          javafx.concurrent.Worker.State oldValue,
          javafx.concurrent.Worker.State newValue) {

        if (Worker.State.SUCCEEDED.equals(newValue)) {
          NodeList nodeList = webEngine.getDocument().getElementsByTagName("a");
          for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            EventTarget eventTarget = (EventTarget) node;
            eventTarget.addEventListener("click", new EventListener() {
              @Override
              public void handleEvent(Event evt) {
                evt.preventDefault();
                EventTarget target = evt.getCurrentTarget();
                HTMLAnchorElement anchorElement = (HTMLAnchorElement) target;
                final String href = anchorElement.getHref();
                SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                    try {
                      Desktop.getDesktop().browse(new URI(href));
                    } catch (IOException | URISyntaxException e) {
                      GPLogger.log(e);
                    }
                  }
                });
              }
            }, false);
          }
        }
      }
    });
  }
  private String buildStartDateChangeTable() {
    List<String> tableRows = Lists.newArrayList();
    for (Entry<Task, Pair<Date, Date>> entry : myModifiedTasks.entrySet()) {
      Task t = entry.getKey();
      Pair<Date,Date> changes = entry.getValue();
      if (changes.first() != null) {
        String row = String.format(i18n.getText("scheduler.warning.table.row"),
            t.getName(),
            i18n.formatDate(CalendarFactory.createGanttCalendar(changes.first())),
            i18n.getText(Objects.firstNonNull(
                myReasons.get(t),
                "scheduler.warning.table.reason.other") + ".url"),
            i18n.getText(Objects.firstNonNull(
                myReasons.get(t),
                "scheduler.warning.table.reason.other") + ".label")
         );
        tableRows.add(row);
      }
    }
    String rows =  Joiner.on('\n').join(tableRows);
    String table = String.format(i18n.getText("scheduler.warning.table.template"),
        i18n.getText("scheduler.warning.section.startDate.title"),
        i18n.getText("scheduler.warning.section.startDate.desc"),
        i18n.getText("taskname"),
        i18n.getText("option.generic.startDate.label"),
        i18n.getText("scheduler.warning.table.reason"),
        rows);
    return table;
  }
  private String buildEndDateChangeTable() {
    List<String> tableRows = Lists.newArrayList();
    for (Entry<Task, Pair<Date, Date>> entry : myModifiedTasks.entrySet()) {
      Task t = entry.getKey();
      Pair<Date,Date> changes = entry.getValue();
      if (changes.first() == null) {
        String row = String.format(i18n.getText("scheduler.warning.table.row"),
            t.getName(),
            i18n.formatDate(CalendarFactory.createGanttCalendar(changes.second())),
            i18n.getText(Objects.firstNonNull(
                myReasons.get(t),
                "scheduler.warning.table.reason.other") + ".url"),
            i18n.getText(Objects.firstNonNull(
                myReasons.get(t),
                "scheduler.warning.table.reason.other") + ".label")
         );
        tableRows.add(row);
      }
    }
    String rows =  Joiner.on('\n').join(tableRows);
    String table = String.format(i18n.getText("scheduler.warning.table.template"),
        i18n.getText("scheduler.warning.section.endDate.title"),
        i18n.getText("scheduler.warning.section.endDate.desc"),
        i18n.getText("taskname"),
        i18n.getText("option.generic.endDate.label"),
        i18n.getText("scheduler.warning.table.reason"),
        rows);
    return table;
  }
}