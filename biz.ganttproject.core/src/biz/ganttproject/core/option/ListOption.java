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
package biz.ganttproject.core.option;

/**
 * Enumeration option which allows for adding new values. It is typically
 * represented as editable drop-down in the UI
 *
 * @author dbarashev (Dmitry Barashev)
 */
public interface ListOption<T> extends GPOption<T> {
  void setValues(Iterable<T> values);
  Iterable<T> getValues();
  void setValueIndex(int idx);
  void addValue(T value);
  void updateValue(T oldValue, T newValue);
  void removeValueIndex(int idx);

  EnumerationOption asEnumerationOption();
}
