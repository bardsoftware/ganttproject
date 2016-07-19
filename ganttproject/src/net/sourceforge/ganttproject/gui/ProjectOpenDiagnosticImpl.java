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

import biz.ganttproject.core.time.CalendarFactory;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmBase;
import net.sourceforge.ganttproject.util.collect.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Diagnostic class collects information from the scheduler when user opens
 * a project. Should we have any changes in the task dates after the first scheduler
 * run, we report them to the user.
 *
 * @author dbarashev@ganttproject.biz (Dmitry Barashev)
 */
class ProjectOpenDiagnosticImpl implements AlgorithmBase.Diagnostic {
  interface ShowDialogCallback {
    void showDialog(JComponent contentPane);
  }
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
    final ShowDialogCallback showDialog = new ShowDialogCallback() {
      @Override
      public void showDialog(JComponent contentPane) {
        Dimension htmlSize = contentPane.getPreferredSize();
        final JScrollPane scrollPane = new JScrollPane(contentPane);
        scrollPane.setAutoscrolls(false);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(Math.min(600, htmlSize.width + 50), Math.min(400, htmlSize.height + 50)));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 5, 3));
        myUiFacade.createDialog(scrollPane, new Action[] {CancelAction.CLOSE}, "").show();
      }
    };
    try {
      Class.forName("javafx.beans.value.ChangeListener");
      new ProjectOpenDiagnosticUiFx().run(msg, showDialog);
    } catch (ClassNotFoundException e) {
      new ProjectOpenDiagnosticUiSwing().run(msg, showDialog);
    }
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