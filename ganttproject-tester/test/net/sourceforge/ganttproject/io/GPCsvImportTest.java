package net.sourceforge.ganttproject.io;

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.TestSetupHelper.TaskManagerBuilder;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;

/**
 * Tests CSV import with GP semantics
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class GPCsvImportTest extends TestCase {
  private Supplier<Reader> createSupplier(String data) {
    return Suppliers.<Reader> ofInstance(new StringReader(data));
  }

  public void testImportAssignments() throws Exception {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    TaskManager taskManager = builder.build();
    HumanResourceManager resourceManager = builder.getResourceManager();

    String header1 = "Name,Begin date,End date,Resources,Duration,Completion,Web Link,Notes";
    String data1 = "t1,23/07/12,25/07/12,Joe;John,,,,";

    String header2 = "Name,ID,e-mail,Phone,Default role";
    String data2 = "Joe,1,,,\nJohn,2,,,\nJack,3,,,";
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header1, data1, "", header2, data2)), taskManager, resourceManager);
    importer.load();

    Map<String, Task> taskMap = Maps.uniqueIndex(Arrays.asList(taskManager.getTasks()), new Function<Task, String>() {
      @Override
      public String apply(Task input) {
        return input.getName();
      }
    });
    Task t1 = taskMap.get("t1");
    assertNotNull(t1);
    Map<String, HumanResource> resourceMap = Maps.uniqueIndex(resourceManager.getResources(), new Function<HumanResource, String>() {
      @Override
      public String apply(HumanResource input) {
        return input.getName();
      }
    });
    assertNotNull(t1.getAssignmentCollection().getAssignment(resourceMap.get("Joe")));
    assertNotNull(t1.getAssignmentCollection().getAssignment(resourceMap.get("John")));
  }
}
