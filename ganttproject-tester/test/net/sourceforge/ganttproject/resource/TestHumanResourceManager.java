package net.sourceforge.ganttproject.resource;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.task.CustomColumnsManager;


public class TestHumanResourceManager extends TestCase {
  public void testUpResource() {
    HumanResourceManager manager = new HumanResourceManager(null, new CustomColumnsManager());
    var resource1 = manager.newHumanResource();
    resource1.setName("TEST_RESSOURCE1");
    manager.add(resource1);

    var resource2 = manager.newHumanResource();
    resource2.setName("TEST_RESSOURCE2");
    manager.add(resource2);

    assertEquals(0, manager.getResources().indexOf(resource1));
    assertEquals(1, manager.getResources().indexOf(resource2));

    manager.up(resource2);

    assertEquals(1, manager.getResources().indexOf(resource1));
    assertEquals(0, manager.getResources().indexOf(resource2));

  }
  public void testDownResource() {
    // Arrange
    HumanResourceManager manager = new HumanResourceManager(null, new CustomColumnsManager());
    var resource1 = manager.newHumanResource();
    resource1.setName("TEST_RESSOURCE1");
    manager.add(resource1);

    var resource2 = manager.newHumanResource();
    resource2.setName("TEST_RESSOURCE2");
    manager.add(resource2);

    assertEquals(0, manager.getResources().indexOf(resource1));
    assertEquals(1, manager.getResources().indexOf(resource2));

    manager.down(resource1);

    assertEquals(1, manager.getResources().indexOf(resource1));
    assertEquals(0, manager.getResources().indexOf(resource2));
  }

  public void testClear() {
    HumanResourceManager manager = new HumanResourceManager(null, new CustomColumnsManager());
    var resource1 = manager.newHumanResource();
    manager.add(resource1);

    var resource2 = manager.newHumanResource();
    manager.add(resource2);

    assertEquals(2, manager.getResources().size());

    manager.clear();

    assertEquals(0, manager.getResources().size());

  }
}
