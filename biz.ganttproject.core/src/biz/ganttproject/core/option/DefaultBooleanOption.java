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
package biz.ganttproject.core.option;

public class DefaultBooleanOption extends GPAbstractOption<Boolean> implements BooleanOption {

  public DefaultBooleanOption(String id) {
    super(id);
  }

  public DefaultBooleanOption(String id, boolean initialValue) {
    super(id, initialValue);
  }

  @Override
  public boolean isChecked() {
    return getValue();
  }

  @Override
  public Boolean getValue() {
    return super.getValue() == null ? Boolean.FALSE : super.getValue();
  }

  @Override
  public void toggle() {
    setValue(!getValue());
  }

  @Override
  public String getPersistentValue() {
    return Boolean.toString(isChecked());
  }

  @Override
  public void loadPersistentValue(String value) {
    resetValue(Boolean.valueOf(value).booleanValue(), true);
  }

}
