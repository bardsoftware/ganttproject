/*
Copyright 2012 GanttProject Team

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
package net.sourceforge.ganttproject.roles;

import junit.framework.TestCase;

/**
 * RoleSet implementation tests
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class RoleSetTest extends TestCase {
  private RoleManagerImpl myRoleManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myRoleManager = new RoleManagerImpl();
  }

  public void testEmpty() {
    RoleSet roleSet = new RoleSetImpl("test", myRoleManager);
    assertTrue(roleSet.isEmpty());
    assertEquals(0, roleSet.getRoles().length);
  }

  public void testCreateRole() {
    RoleSet roleSet = new RoleSetImpl("test", myRoleManager);
    roleSet.createRole("role1");
    assertFalse(roleSet.isEmpty());
    assertEquals(1, roleSet.getRoles().length);
    assertEquals("role1", roleSet.getRoles()[0].getName());
    assertEquals("test:1", roleSet.getRoles()[0].getPersistentID());

    roleSet.createRole("role2", 5);
    assertEquals(2, roleSet.getRoles().length);
    assertEquals("role2", roleSet.getRoles()[1].getName());
    assertEquals(5, roleSet.getRoles()[1].getID());
    assertEquals("test:5", roleSet.getRoles()[1].getPersistentID());
  }

  public void testDeleteRole() {
    RoleSet roleSet = new RoleSetImpl("test", myRoleManager);
    Role role = roleSet.createRole("role1");
    roleSet.deleteRole(role);
    assertTrue(roleSet.isEmpty());
    assertEquals(0, roleSet.getRoles().length);
  }

  public void testAutoIdsAreUnique() {
    RoleSet roleSet = new RoleSetImpl("test", myRoleManager);
    roleSet.createRole("role1", 1);
    Role role2 = roleSet.createRole("role2", 2);
    roleSet.createRole("role3", 3);
    roleSet.deleteRole(role2);

    Role role4 = roleSet.createRole("role4");
    assertEquals(4, role4.getID());
  }
}
