/*
Copyright 2014 BarD Software s.r.o

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

import java.util.List;

/**
 * Default implementation of FontOption
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class DefaultFontOption extends GPAbstractOption<FontSpec> implements FontOption {
  private final List<String> myFontFamilies;

  public DefaultFontOption(String id, FontSpec initialValue, List<String> families) {
    super(id, initialValue);
    myFontFamilies = families;
  }

  @Override
  public String getPersistentValue() {
    return getValue().asString();
  }

  @Override
  public void loadPersistentValue(String value) {
  }

  @Override
  public List<String> getFontFamilies() {
    return myFontFamilies;
  }
}
