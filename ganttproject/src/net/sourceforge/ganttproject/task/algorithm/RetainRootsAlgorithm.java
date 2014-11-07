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
package net.sourceforge.ganttproject.task.algorithm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

/**
 * Examines the given array of nodes and retains only those
 * whose parents are not in the input array.
 *
 * @author dbarashev
 *
 * @param <T> Node type
 */
public class RetainRootsAlgorithm<T> {
  public void run(T[] nodes, Function<T, T> getParent, Collection<T> output) {
    run(Arrays.asList(nodes), getParent, output);
  }

  public void run(Collection<T> nodes, Function<T, T> getParent, Collection<T> output) {
    final Set<T> set = Sets.newHashSet(nodes);
    for (T node : nodes) {
      for (T parent = getParent.apply(node); parent != null; parent = getParent.apply(parent)) {
        if (set.contains(parent)) {
          set.remove(node);
          break;
        }
      }
    }
    output.addAll(set);
  }
}
