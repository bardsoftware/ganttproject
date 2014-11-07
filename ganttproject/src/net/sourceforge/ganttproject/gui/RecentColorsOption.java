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
package net.sourceforge.ganttproject.gui;

import java.awt.Color;
import java.util.List;

import biz.ganttproject.core.option.ColorOption.Util;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPAbstractOption;
import biz.ganttproject.core.option.ListOption;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Option class for working with a list of colors.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class RecentColorsOption extends GPAbstractOption<Color> implements ListOption<Color> {
  private final List<Color> myColors;

  public RecentColorsOption(List<Color> colors) {
    super("color.recent");
    myColors = Preconditions.checkNotNull(colors);
  }

  @Override
  public void setValue(Color value) {
    if (!Objects.equal(value, getValue())) {
      super.setValue(value);
    }
  }


  @Override
  public String getPersistentValue() {
    if (myColors.isEmpty()) {
      return null;
    }
    List<String> values = Lists.transform(myColors, new Function<Color, String>() {
      @Override
      public String apply(Color c) {
        return Util.getColor(c);
      }
    });
    return Joiner.on(' ').join(values);
  }

  @Override
  public void loadPersistentValue(String value) {
    myColors.clear();
    String[] values = value.trim().split("\\s+");
    for (String strColor : values) {
      Color color = Util.determineColor(strColor);
      myColors.add(color);
    }
  }

  @Override
  public void setValues(Iterable<Color> values) {
    myColors.clear();
    myColors.addAll(Lists.newArrayList(values));
  }

  @Override
  public Iterable<Color> getValues() {
    return myColors;
  }

  @Override
  public void setValueIndex(int idx) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addValue(Color value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateValue(Color oldValue, Color newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeValueIndex(int idx) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EnumerationOption asEnumerationOption() {
    throw new UnsupportedOperationException();
  }

}
