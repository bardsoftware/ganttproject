/*
Copyright 2014 BarD Software s.r.o

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
package net.sourceforge.ganttproject.chart.gantt;

import biz.ganttproject.customproperty.CustomPropertyDefinition;
import net.sourceforge.ganttproject.AbstractChartImplementation.ChartSelectionImpl;
import net.sourceforge.ganttproject.GPTransferable;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static biz.ganttproject.task.TreeAlgorithmsKt.retainRoots;

/**
 * Implementation of ChartSelection on Gantt chart.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class GanttChartSelection extends ChartSelectionImpl implements ClipboardOwner {

  private final TaskManager myTaskManager;
  private final TaskSelectionManager mySelectionManager;

  private ClipboardContents myClipboardContents;

  GanttChartSelection(TaskManager taskManager, TaskSelectionManager selectionManager) {
    myTaskManager = taskManager;
    mySelectionManager = selectionManager;
  }
  @Override
  public boolean isEmpty() {
    return mySelectionManager.getSelectedTasks().isEmpty();
  }

  @Override
  public void startCopyClipboardTransaction() {
    super.startCopyClipboardTransaction();
    myClipboardContents = buildClipboardContents(myTaskManager);
    myClipboardContents.copy();
    exportTasksIntoSystemClipboard();
  }

  private void exportTasksIntoSystemClipboard() {
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    // We create a copy of the task model here, to allow for manipulations with the tasks when builing
    // an instance of the external document flavor.
    // See issue https://github.com/bardsoftware/ganttproject/issues/2050
    // Test case: GanttChartSelectionTest::testStartMoveTransactionAndExternalDocumentFlavor_Issue2050
    var exportedTaskManager = myTaskManager.emptyClone();
    var customDefMap = new HashMap<CustomPropertyDefinition, CustomPropertyDefinition>();
    for (var def : myTaskManager.getCustomPropertyManager().getDefinitions()) {
      customDefMap.put(def, def);
    }
    exportedTaskManager.importData(myTaskManager, customDefMap);
    // we build a mapping of the currently selected tasks to their copies in the external task model
    var selectedRoots = retainRoots(mySelectionManager.getSelectedTasks().stream()
      .map(t -> exportedTaskManager.getTask(t.getTaskID())).collect(Collectors.toList()));
    var clipboardContents = new ClipboardContents(exportedTaskManager);
    clipboardContents.addTasks(selectedRoots);
    clipboardContents.copy();
    clipboard.setContents(new GPTransferable(clipboardContents), this);
  }

  @Override
  public void startMoveClipboardTransaction() {
    super.startMoveClipboardTransaction();
    myClipboardContents = buildClipboardContents(myTaskManager);
    myClipboardContents.cut();
    if (!GraphicsEnvironment.isHeadless()) {
      exportTasksIntoSystemClipboard();
    }
  }

  public ClipboardContents buildClipboardContents(TaskManager taskManager) {
    List<Task> selectedRoots = retainRoots(mySelectionManager.getSelectedTasks());
    ClipboardContents result = new ClipboardContents(taskManager);
    result.addTasks(selectedRoots);
    return result;
  }

  List<Task> paste(Task target) {
    if (myClipboardContents == null) {
      return Collections.emptyList();
    }
    ClipboardTaskProcessor processor = new ClipboardTaskProcessor(myTaskManager);
    processor.setTaskCopyNameOption(myTaskManager.getTaskCopyNamePrefixOption());
    return processor.pasteAsSibling(target, myClipboardContents);
  }

  @Override
  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    // Do nothing
  }

  @Override
  public String toString() {
    return "GanttChartSelection{" +
      "myClipboardContents=" + myClipboardContents +
      "mySelectedTasks=" + mySelectionManager.getSelectedTasks() +
      '}';
  }
}
