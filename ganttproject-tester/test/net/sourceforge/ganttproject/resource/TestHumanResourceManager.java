/*
Copyright 2021 BarD Software s.r.o, Emile Dupont-Foisy

This file is part of GanttProject, an open-source project management tool.

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
