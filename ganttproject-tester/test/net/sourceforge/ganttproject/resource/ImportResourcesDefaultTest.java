/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject.resource;

import biz.ganttproject.customproperty.CustomColumnsManager;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.TestSetupHelper.TaskManagerBuilder;
import net.sourceforge.ganttproject.resource.HumanResourceMerger.MergeResourcesOption;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * Resource IDs are numbered from 0 in every project file which GanttProject writes. Two files which
 * were created independently therefore collide on resource #0 by construction, even though they
 * describe different people. With the duplicate detection defaulting to "by ID", importing such a
 * file replaced a resource of the target project: its name, mail, phone, role and pay rate were
 * overwritten with the values of a stranger, the assignments of the target project kept pointing at
 * that record, and nothing was reported to the user.
 *
 * These tests use a freshly constructed {@link MergeResourcesOption}, that is, they test the
 * *default* value of the option. A user who explicitly chooses "by ID" still gets the overwriting
 * behaviour; that is covered by {@code TestImportResources#testMergeByID}.
 */
public class ImportResourcesDefaultTest extends TaskTestCase {

  private static HumanResource addResource(
      HumanResourceManager mgr, String name, int id, String mail, String rate) {
    HumanResource result = new HumanResource(name, id, mgr);
    result.setMail(mail);
    result.setStandardPayRate(new BigDecimal(rate));
    mgr.add(result);
    return result;
  }

  private static HumanResourceManager newResourceManager() {
    return new HumanResourceManager(null, new CustomColumnsManager());
  }

  /**
   * The imported resource must not take over the record of a resource of the target project just
   * because both happen to be resource #0 in their own file.
   */
  public void testDefaultDoesNotOverwriteResourceWithTheSameId() {
    HumanResourceManager target = newResourceManager();
    addResource(target, "Jane Target", 0, "jane@target.example", "100");

    HumanResourceManager imported = newResourceManager();
    addResource(imported, "John Acquired", 0, "john@acquired.example", "50");

    target.importData(imported, new OverwritingMerger(new MergeResourcesOption()), Collections.emptyMap());

    assertEquals("The imported resource must be added, not merged into the existing one",
        2, target.getResources().size());
    HumanResource jane = target.getById(0);
    assertNotNull("The resource of the target project has disappeared", jane);
    assertEquals("The name of the target project's resource was overwritten",
        "Jane Target", jane.getName());
    assertEquals("The mail address of the target project's resource was overwritten",
        "jane@target.example", jane.getMail());
    assertEquals("The pay rate of the target project's resource was overwritten",
        100.0, jane.getStandardPayRate().doubleValue(), 0.0001);
  }

  /**
   * The lasting damage: the numbers of a task which belongs to the target project, and which the
   * user had checked before the import, change behind his back.
   */
  public void testDefaultKeepsTheCostOfTheTargetProjectsOwnTask() {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    setTaskManager(builder.build());
    HumanResourceManager target = builder.getResourceManager();
    HumanResource jane = addResource(target, "Jane Target", 0, "jane@target.example", "100");

    Task ownTask = createTask();
    ownTask.setDuration(ownTask.getManager().createLength(5));
    ownTask.getAssignmentCollection().addAssignment(jane).setLoad(100f);
    assertEquals("Precondition: 5 days at 100% and a rate of 100",
        500.0, jane.getTotalCost().doubleValue(), 0.0001);

    HumanResourceManager imported = newResourceManager();
    addResource(imported, "John Acquired", 0, "john@acquired.example", "50");

    target.importData(imported, new OverwritingMerger(new MergeResourcesOption()), Collections.emptyMap());

    assertEquals("The cost of the target project's own task changed because of the import",
        500.0, jane.getTotalCost().doubleValue(), 0.0001);
    HumanResource assignee = ownTask.getAssignments()[0].getResource();
    assertEquals("The task of the target project is assigned to someone else after the import",
        "Jane Target", assignee.getName());
  }

  /**
   * Neither the name nor the mail address of a resource is mandatory: a resource created with
   * Ctrl+H and confirmed right away has an empty name and no mail address. Two such resources are
   * not the same person, and must not be matched on their empty value.
   */
  public void testBlankNamesAreNotTreatedAsTheSamePerson() {
    HumanResourceManager target = newResourceManager();
    addResource(target, "", 0, "", "100");

    HumanResourceManager imported = newResourceManager();
    addResource(imported, "", 0, "", "50");

    target.importData(imported, new OverwritingMerger(new MergeResourcesOption()), Collections.emptyMap());

    assertEquals("Two nameless resources were treated as the same person",
        2, target.getResources().size());
    assertEquals("The pay rate of the target project's resource was overwritten",
        100.0, target.getById(0).getStandardPayRate().doubleValue(), 0.0001);
  }

  /**
   * Same for the mail address, which is the other field a user can leave empty.
   */
  public void testBlankMailAddressesAreNotTreatedAsTheSamePerson() {
    MergeResourcesOption byMail = new MergeResourcesOption();
    byMail.setSelectedValue(MergeResourcesEnum.BY_EMAIL);

    HumanResourceManager target = newResourceManager();
    addResource(target, "Jane Target", 0, null, "100");

    HumanResourceManager imported = newResourceManager();
    addResource(imported, "John Acquired", 0, null, "50");

    target.importData(imported, new OverwritingMerger(byMail), Collections.emptyMap());

    assertEquals("Two resources without a mail address were treated as the same person",
        2, target.getResources().size());
    assertEquals("The name of the target project's resource was overwritten",
        "Jane Target", target.getById(0).getName());
  }

  /**
   * The point of the option is not lost: when both files really do describe the same person, the
   * records are still merged, and the differing IDs do not get in the way.
   */
  public void testDefaultStillMergesTheSamePersonAcrossFiles() {
    HumanResourceManager target = newResourceManager();
    addResource(target, "Jane Target", 0, "jane@target.example", "100");

    HumanResourceManager imported = newResourceManager();
    addResource(imported, "Jane Target", 7, "jane@elsewhere.example", "120");

    target.importData(imported, new OverwritingMerger(new MergeResourcesOption()), Collections.emptyMap());

    assertEquals("The same person must not be duplicated", 1, target.getResources().size());
    assertEquals("Jane Target", target.getResources().get(0).getName());
  }
}
