/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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
package net.sourceforge.ganttproject.gui.options.model;

import java.util.Arrays;
import java.util.LinkedHashSet;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Default implementation of ListOption
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class DefaultStringListOption extends GPAbstractOption<String> implements ListOption {
  private LinkedHashSet<String> myValues = Sets.newLinkedHashSet();

  public DefaultStringListOption(String id) {
    super(id);
  }

  @Override
  public String[] getAvailableValues() {
    return myValues.toArray(new String[myValues.size()]);
  }

  @Override
  public String getPersistentValue() {
    return Joiner.on('\n').join(myValues);
  }

  @Override
  public void loadPersistentValue(String value) {
    myValues = Sets.newLinkedHashSet(Arrays.asList(value.split("\n")));
  }

  @Override
  public void addValue(String value) {
    myValues.add(value);
  }

  @Override
  public void removeValue(String value) {
    myValues.remove(value);
  }

  @Override
  public void setValues(Iterable<String> values) {
    myValues.clear();
    myValues.addAll(Lists.newArrayList(values));
  }

}
