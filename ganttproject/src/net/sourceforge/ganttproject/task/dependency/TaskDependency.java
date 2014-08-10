/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.task.dependency;

import java.util.Date;

import biz.ganttproject.core.chart.scene.BarChartConnector;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;

/**
 * Created by IntelliJ IDEA. User: bard Date: 14.02.2004 Time: 2:32:17 To change
 * this template use File | Settings | File Templates.
 */
public interface TaskDependency extends BarChartConnector<Task, TaskDependency> {
  abstract class Hardness {
    public static final Hardness RUBBER = new Hardness("Rubber") {
      @Override
      public String toString() {
        return GanttLanguage.getInstance().getText("hardness.rubber");
      }
    };
    public static final Hardness STRONG = new Hardness("Strong") {
      @Override
      public String toString() {
        return GanttLanguage.getInstance().getText("hardness.strong");
      }
    };

    public static Hardness parse(String hardnessAsString) {
      if (hardnessAsString == null) {
        throw new IllegalArgumentException("Null value is not allowed as hardness");
      }
      if ("Rubber".equals(hardnessAsString.trim())) {
        return RUBBER;
      }
      if ("Strong".equals(hardnessAsString.trim())) {
        return STRONG;
      }
      throw new IllegalArgumentException("Unexpected hardness string value=" + hardnessAsString);
    }

    private String myID;

    private Hardness(String id) {
      myID = id;
    }

    public String getIdentifier() {
      return myID;
    }
  }

  Task getDependant();

  Task getDependee();

  void setConstraint(TaskDependencyConstraint constraint);

  TaskDependencyConstraint getConstraint();

  ActivityBinding getActivityBinding();

  void delete();

  interface ActivityBinding {
    TaskActivity getDependantActivity();

    TaskActivity getDependeeActivity();

    Date[] getAlignedBounds();
  }

  int getDifference();

  void setDifference(int difference);

  Hardness getHardness();

  void setHardness(Hardness hardness);
}
