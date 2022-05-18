package net.sourceforge.ganttproject.test.task;

import biz.ganttproject.core.time.GanttCalendar;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmBase;

import java.util.Date;

public class TestTaskBounds extends TaskTestCase {
  public void testIssue953() {
    GanttCalendar monday = TestSetupHelper.newMonday();
    GanttCalendar tuesday = TestSetupHelper.newTuesday();
    GanttCalendar wednesday = TestSetupHelper.newWendesday();

    Task supertask = createTask(monday);
    Task childMilestone1 = createTask(monday);
    Task childMilestone2 = createTask(tuesday);
    childMilestone1.setMilestone(true);
    childMilestone2.setMilestone(true);
    childMilestone1.move(supertask);
    childMilestone2.move(supertask);

    getTaskManager().getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(supertask);
    assertEquals(monday, supertask.getStart());
    assertEquals(monday, supertask.getDisplayEnd());

    getTaskManager().getAlgorithmCollection().getScheduler().setDiagnostic(new AlgorithmBase.Diagnostic() {
      @Override
      public void addModifiedTask(Task t, Date newStart, Date newEnd) {

      }

      @Override
      public void logError(Exception ex) {
        throw new RuntimeException(ex);
      }
    });
    var mutator = childMilestone1.createShiftMutator();
    mutator.shift(getTaskManager().createLength("1d"));
    mutator.commit();
    getTaskManager().getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(supertask);
    assertEquals(tuesday, supertask.getStart());
    assertEquals(tuesday, supertask.getDisplayEnd());

  }
}
