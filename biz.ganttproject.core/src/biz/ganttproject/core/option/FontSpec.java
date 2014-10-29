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

/**
 * Font specification object, encapsulating font family and size.
 * It is used in option infrastructure which is meant to be portable to other
 * platforms, which is the reason why we don't use java.awt.Font objects. 
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class FontSpec {
  private final String myFamily;
  private final int mySize;
  
  public FontSpec(String family, int size) {
    myFamily = family;
    mySize = size;
  }
  
  public String getFamily() {
    return myFamily;
  }
  
  public int getSize() {
    return mySize;
  }
  
  public String asString() {
    return String.format("%s-%d", myFamily, mySize);
  }
}
