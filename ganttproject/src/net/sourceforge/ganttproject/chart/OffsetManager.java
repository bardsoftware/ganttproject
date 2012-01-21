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
package net.sourceforge.ganttproject.chart;

import java.util.List;

import net.sourceforge.ganttproject.chart.ChartModelBase.OffsetBuilderImpl;

/**
 * Holds offset lists and provides a unified way to reset them all at once
 * and to rebuild them again.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class OffsetManager {
    static interface OffsetBuilderFactory {
        OffsetBuilder createTopAndBottomUnitBuilder();
        OffsetBuilderImpl createAtomUnitBuilder();
    }

    private final OffsetList myTopUnitOffsets = new OffsetList();
    private final OffsetList myBottomUnitOffsets = new OffsetList();
    private final OffsetList myDefaultUnitOffsets = new OffsetList();
    private final OffsetBuilderFactory myFactory;
    private boolean isReset = true;

    OffsetManager(OffsetBuilderFactory factory) {
        myFactory = factory;
    }

    void reset() {
        isReset = true;
    }

    void constructOffsets() {
        myTopUnitOffsets.clear();
        myBottomUnitOffsets.clear();
        myDefaultUnitOffsets.clear();
        myFactory.createTopAndBottomUnitBuilder().constructOffsets(myTopUnitOffsets, myBottomUnitOffsets);
        // this is a hack which prevents an eternal loop of calling constructOffsets.
        // The matter is that atom unit builder calls getEndDate() which in turn calls
        // constructOffsets()
        isReset = false;
        myFactory.createAtomUnitBuilder().constructBottomOffsets(myDefaultUnitOffsets, 0);
    }

    public List<Offset> getTopUnitOffsets() {
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
