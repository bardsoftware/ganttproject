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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.JOptionPane;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmBase;
import net.sourceforge.ganttproject.util.collect.Pair;
import biz.ganttproject.core.time.CalendarFactory;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

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
    String intro = Joiner.on("<br>").join(myMessages);
    String startDateChangeTable = buildStartDateChangeTable();
    String endDateChangeTable = myHasOnlyEndDateChange ? buildEndDateChangeTable() : "";
    String reasonTable = buildReasonTable();
    String msg = String.format("<html><p>%s</p><br>%s%s<br>%s</html>",
        intro,
        startDateChangeTable,
        endDateChangeTable,
        reasonTable);
    myUiFacade.showOptionDialog(JOptionPane.INFORMATION_MESSAGE, msg, new Action[] {CancelAction.CLOSE});
  }
  private String buildReasonTable() {
    List<String> rows = Lists.newArrayList();
    Set<String> uniqueReasons = new LinkedHashSet<>(myReasons.values());
    uniqueReasons.add("scheduler.warning.reason.other");
    for (String reasonKey : uniqueReasons) {
      rows.add(String.format("<p><b>%s</b>: %s<br></p>",
          i18n.getText(reasonKey + ".label"),
          i18n.getText(reasonKey + ".description")));
    }
    return String.format("<hr>%s", Joiner.on("<br>").join(rows));
  }
  private String buildStartDateChangeTable() {
    List<String> tableRows = Lists.newArrayList();
    for (Entry<Task, Pair<Date, Date>> entry : myModifiedTasks.entrySet()) {
      Task t = entry.getKey();
      Pair<Date,Date> changes = entry.getValue();
      if (changes.first() != null) {
        String row = String.format("<tr><td>%s</td><td>%s</td><td>%s</td></tr>",
            t.getName(),
            i18n.formatDate(CalendarFactory.createGanttCalendar(changes.first())),
            i18n.getText(Objects.firstNonNull(
                myReasons.get(t),
                "scheduler.warning.reason.other") + ".label")
         );
        tableRows.add(row);
      }
    }
    String rows =  Joiner.on('\n').join(tableRows);
    String table = String.format("<hr><b>%s</b><table><tr><th>%s</th><th>%s</th><th>%s</th></tr>%s</table>",
        i18n.getText("scheduler.warning.section.startDate"),
        i18n.getText("taskname"),
        i18n.getText("option.generic.startDate.label"),
        i18n.getText("scheduler.warning.reason"),
        rows);
    return table;
  }
  private String buildEndDateChangeTable() {
    List<String> tableRows = Lists.newArrayList();
    for (Entry<Task, Pair<Date, Date>> entry : myModifiedTasks.entrySet()) {
      Task t = entry.getKey();
      Pair<Date,Date> changes = entry.getValue();
      if (changes.first() == null) {
        String row = String.format("<br><tr><td>%s</td><td>%s</td><td>%s</td></tr>",
            t.getName(),
            i18n.formatDate(CalendarFactory.createGanttCalendar(changes.second())),
            i18n.getText(Objects.firstNonNull(
                myReasons.get(t),
                "scheduler.warning.reason.other") + ".label")
         );
        tableRows.add(row);
      }
    }
    String rows =  Joiner.on('\n').join(tableRows);
    String table = String.format("<b>%s</b><table><tr><th>%s</th><th>%s</th><th>%s</th></tr>%s</table>",
        i18n.getText("scheduler.warning.section.endDate"),
        i18n.getText("taskname"),
        i18n.getText("option.generic.endDate.label"),
        i18n.getText("scheduler.warning.reason"),
        rows);
    return table;
  }
}