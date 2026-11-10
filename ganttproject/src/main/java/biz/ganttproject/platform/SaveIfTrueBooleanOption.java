/*
Copyright 2026 BarD Software s.r.o

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
package biz.ganttproject.platform;

import biz.ganttproject.core.option.DefaultBooleanOption;

/**
 * Boolean option with the default value of false, which is written to the settings file only when it is true.
 * This keeps hidden options out of regular users' settings files.
 *
 * It is written in Java because a Kotlin subclass of DefaultBooleanOption clashes with GPObservable.value.
 */
public class SaveIfTrueBooleanOption extends DefaultBooleanOption {
  public SaveIfTrueBooleanOption(String id) {
    super(id, false);
  }

  @Override
  public String getPersistentValue() {
    return isChecked() ? super.getPersistentValue() : null;
  }
}
