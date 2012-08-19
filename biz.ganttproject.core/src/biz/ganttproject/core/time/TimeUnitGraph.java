/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package biz.ganttproject.core.time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 01.02.2004
 */
public class TimeUnitGraph {
  private Map<TimeUnit, List<Composition>> myUnit2compositions = new HashMap<TimeUnit, List<Composition>>();

  public TimeUnit createAtomTimeUnit(String name) {
    TimeUnit result = new TimeUnitImpl(name, this, null);
    List<Composition> compositions = new ArrayList<Composition>();
    compositions.add(new Composition(result, 1));
    myUnit2compositions.put(result, compositions);
    return result;
  }

  TimeUnit createTimeUnit(String name, TimeUnit atomUnit, int count) {
    TimeUnit result = new TimeUnitImpl(name, this, atomUnit);
    registerTimeUnit(result, count);
    return result;
  }

  public TimeUnit createDateFrameableTimeUnit(String name, TimeUnit atomUnit, int atomCount, DateFrameable framer) {
    TimeUnit result = new TimeUnitDateFrameableImpl(name, this, atomUnit, framer);
    registerTimeUnit(result, atomCount);
    return result;
  }

  public TimeUnitFunctionOfDate createTimeUnitFunctionOfDate(String name, TimeUnit atomUnit, DateFrameable framer) {
    TimeUnitFunctionOfDate result;
    result = new TimeUnitFunctionOfDateImpl(name, this, atomUnit, framer);
    registerTimeUnit(result, -1);
    return result;
  }

  private void registerTimeUnit(TimeUnit unit, int atomCount) {
    TimeUnit atomUnit = unit.getDirectAtomUnit();
    List<Composition> transitiveCompositions = myUnit2compositions.get(atomUnit);
    if (transitiveCompositions == null) {
      throw new RuntimeException("Atom unit=" + atomUnit + " is unknown");
    }
    List<Composition> compositions = new ArrayList<Composition>(transitiveCompositions.size() + 1);
    compositions.add(new Composition(unit, 1));
    for (int i = 0; i < transitiveCompositions.size(); i++) {
      Composition nextTransitive = transitiveCompositions.get(i);
      compositions.add(new Composition(nextTransitive, atomCount));
    }
    myUnit2compositions.put(unit, compositions);
  }

  public Composition getComposition(TimeUnitImpl timeUnit, TimeUnit atomUnit) {
    Composition result = null;
    List<Composition> compositions = myUnit2compositions.get(timeUnit);
    if (compositions == null) {
      throw new RuntimeException("Unit=" + timeUnit + " has no compositions");
    }
    for (int i = 0; i < compositions.size(); i++) {
      Composition next = compositions.get(i);
      if (next.myAtom.equals(atomUnit)) {
        result = next;
        break;
      }
    }
    return result;
  }

  class Composition {
    final TimeUnit myAtom;

    final int myAtomCount;

    Composition(TimeUnit atomUnit, int atomCount) {
      myAtom = atomUnit;
      myAtomCount = atomCount;
    }

    Composition(Composition transitiveComposition, int atomCount) {
      myAtomCount = atomCount * transitiveComposition.myAtomCount;
      myAtom = transitiveComposition.myAtom;
    }

    int getAtomCount() {
      return myAtomCount;
    }
  }

}
