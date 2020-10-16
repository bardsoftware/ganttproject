/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Dmitry Barashev, GanttProject Team

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
package biz.ganttproject.core.chart.grid;

/**
 * Holds offset lists and provides a unified way to reset them all at once and
 * to rebuild them again.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class OffsetManager {
  public static interface OffsetBuilderFactory {
    OffsetBuilder createTopAndBottomUnitBuilder();

    OffsetBuilder createAtomUnitBuilder();
  }

  private final OffsetList myTopUnitOffsets = new OffsetList();
  private final OffsetList myBottomUnitOffsets = new OffsetList();
  private final OffsetList myDefaultUnitOffsets = new OffsetList();
  private final OffsetBuilderFactory myFactory;
  private boolean isReset = true;

  public OffsetManager(OffsetBuilderFactory factory) {
    myFactory = factory;
  }

  public void reset() {
    isReset = true;
  }

  public void constructOffsets() {
    myTopUnitOffsets.clear();
    myBottomUnitOffsets.clear();
    myDefaultUnitOffsets.clear();
    myFactory.createTopAndBottomUnitBuilder().constructOffsets(myTopUnitOffsets, myBottomUnitOffsets);
    // this is a hack which prevents an eternal loop of calling
    // constructOffsets.
    // The matter is that atom unit builder calls getEndDate() which in turn
    // calls
    // constructOffsets()
    isReset = false;
    myFactory.createAtomUnitBuilder().constructOffsets(null, myDefaultUnitOffsets);
    alignOffsets(myBottomUnitOffsets);
    alignOffsets(myTopUnitOffsets);
  }

  /**
   * It is possible that different lists get misaligned with respect to the atom unit offsets.
   * For instance, it may happen when chart start date is not on the unit boundary (e.g. bottom unit is MONTH and chart starts
   * somewhere in the middle of a month). We do additional alignment to make sure that offsets which end on the same
   * date have the same pixel offset.
   */
  private void alignOffsets(OffsetList offsets) {
    Offset firstVisibleOffset = null;
    for (Offset o : offsets) {
      if (o.getOffsetPixels() > 0) {
        firstVisibleOffset = o;
        break;
      }
    }
    if (firstVisibleOffset == null) {
      return;
    }
    OffsetLookup lookup = new OffsetLookup();
    int alignedDefaultOffsetIdx = lookup.lookupOffsetByEndDate(firstVisibleOffset.getOffsetEnd(), myDefaultUnitOffsets);
    if (alignedDefaultOffsetIdx >= 0) {
      Offset alignedAtomicOffset = myDefaultUnitOffsets.get(alignedDefaultOffsetIdx);
      int diff = (alignedAtomicOffset.getOffsetPixels() - firstVisibleOffset.getOffsetPixels());
      if (diff == 0) {
        return;
      }
      offsets.shift(diff);
    }
  }

  public OffsetList getTopUnitOffsets() {
    if (isReset) {
      constructOffsets();
    }
    return myTopUnitOffsets;
  }

  public OffsetList getBottomUnitOffsets() {
    if (isReset) {
      constructOffsets();
    }
    return myBottomUnitOffsets;
  }

  public OffsetList getAtomUnitOffsets() {
    if (isReset) {
      constructOffsets();
    }
    return myDefaultUnitOffsets;
  }
}
