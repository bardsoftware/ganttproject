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

import java.util.Objects;

import com.google.common.base.Strings;

/**
 * Font specification object, encapsulating font family and size.
 * It is used in option infrastructure which is meant to be portable to other
 * platforms, which is the reason why we don't use java.awt.Font objects. 
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class FontSpec {
  public static enum Size {
    SMALLER(0.75f), NORMAL(1.0f), LARGE(1.25f), LARGER(1.5f), HUGE(2.0f);
    
    private final float myFactor;

    Size(float factor) {
      myFactor = factor;
    }
    
    public float getFactor() {
      return myFactor;
    }
  }
  private final String myFamily;
  private final Size mySize;
  
  public FontSpec(String family, Size size) {
    myFamily = family;
    mySize = size;
  }
  
  public String getFamily() {
    return myFamily;
  }
  
  public Size getSize() {
    return mySize;
  }

  public String asString() {
    return String.format("%s-%s", Strings.nullToEmpty(myFamily), mySize.toString());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FontSpec == false) {
      return false;
    }
    FontSpec that = (FontSpec) obj;
    return Objects.equals(myFamily, that.myFamily) && Objects.equals(mySize, that.mySize);
  }

  @Override
  public int hashCode() {
    return myFamily.hashCode();
  }

  @Override
  public String toString() {
    return asString();
  }
}
