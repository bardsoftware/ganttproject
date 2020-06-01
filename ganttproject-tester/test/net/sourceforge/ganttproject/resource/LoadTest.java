/*
Copyright 2018 Dmitry Barashev, BarD Software s.r.o

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
import junit.framework.TestCase;

/**
 * @author Laurens Van Damme
 */
public class LoadTest extends TestCase {
  public void testLoadIsUnavailable() {
    LoadDistribution.Load load_day_of = new LoadDistribution.Load(null, null, -1f);
    LoadDistribution.Load load_working = new LoadDistribution.Load(null, null, 100f);

    assertTrue(load_day_of.isResourceUnavailable());
    assertFalse(load_working.isResourceUnavailable());
  }
}
