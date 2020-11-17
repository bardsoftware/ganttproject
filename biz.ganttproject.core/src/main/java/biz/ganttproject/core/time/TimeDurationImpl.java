/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject Team

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
package biz.ganttproject.core.time;

import com.google.common.base.Objects;


/**
 * @author bard
 */
public class TimeDurationImpl implements TimeDuration {
  private final TimeUnit myUnit;

  private float myCount;

  public TimeDurationImpl(TimeUnit unit, long count) {
    myUnit = unit;
    myCount = count;
  }

  /**
   * @param unit
   * @param length
   */
  public TimeDurationImpl(TimeUnit unit, float length) {
    myUnit = unit;
    myCount = length;
  }

  @Override
  public float getValue() {
    return myCount;
  }

  @Override
  public int getLength() {
    return (int) myCount;
  }

  @Override
  public TimeUnit getTimeUnit() {
    return myUnit;
  }

  public void setLength(TimeUnit unit, long length) {
    if (!unit.equals(myUnit)) {
      throw new IllegalArgumentException("Can't convert unit=" + unit + " to my unit=" + myUnit);
    }
    myCount = length;
  }

  @Override
  public float getLength(TimeUnit unit) {
    if (myUnit.isConstructedFrom(unit)) {
      return (float) myCount * myUnit.getAtomCount(unit);
    } else if (unit.isConstructedFrom(myUnit)) {
      return (float) myCount / unit.getAtomCount(myUnit);
    } else if (!unit.equals(myUnit)) {
      throw new IllegalArgumentException("Can't convert unit=" + unit + " to my unit=" + myUnit);
    }
    return myCount;
  }

  @Override
  public TimeDuration reverse() {
    return new TimeDurationImpl(getTimeUnit(), -getLength());
  }

  @Override
  public TimeDuration translate(TimeUnit toUnit) {
    float translatedLength = getLength(toUnit);
    return new TimeDurationImpl(toUnit, translatedLength);
  }

  @Override
  public String toString() {
    return "" + myCount + " " + myUnit.getName();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(myCount, myUnit);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TimeDurationImpl == false) {
      return false;
    }
    TimeDurationImpl that = (TimeDurationImpl) obj;
    return myCount == that.myCount && myUnit.equals(that.myUnit);
  }
  
  
}
