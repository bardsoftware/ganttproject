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
package net.sourceforge.ganttproject.gui.zoom;

import java.util.ArrayList;
import java.util.List;

import biz.ganttproject.core.time.TimeUnitPair;
import biz.ganttproject.core.time.TimeUnitStack;


/**
 * @author bard
 */
public class ZoomManager {
  public static class ZoomState {
    private final TimeUnitPair myTimeUnitPair;

    private final int myBottomUnitWidth;

    private final int myStateNumber;

    ZoomState(TimeUnitPair timeUnitPair, int bottomUnitWidth, int stateNumber) {
      myTimeUnitPair = timeUnitPair;
      myBottomUnitWidth = bottomUnitWidth;
      myStateNumber = stateNumber;
    }

    public ZoomState(TimeUnitPair timeUnitPair, int stateNumber) {
      this(timeUnitPair, timeUnitPair.getDefaultUnitWidth(), stateNumber);
    }

    public String getPersistentName() {
      return myTimeUnitPair.getTimeUnitStack().getName() + ":" + myStateNumber;
    }

    public TimeUnitPair getTimeUnitPair() {
      return myTimeUnitPair;
    }

    public int getBottomUnitWidth() {
      return myBottomUnitWidth;
    }

    public int getValue() {
      return myStateNumber;
    }
  }

  /** Number representing the selected ZoomState */
  private int myZooming = 2;

  private List<ZoomListener> myListeners = new ArrayList<ZoomListener>();

  /**
   * List with available ZoomStates, is expanded when even more ZoomStates are
   * required
   */
  private ArrayList<ZoomState> myZoomStates;

  /** Zoom step for next ZoomState which need to be calculated/extrapolated */
  private final float myZoomStep = 0.75f;

  /** If this value is false the maximum ZoomState is not reached */
  private boolean myMaximumZoomStateReached = false;

  public ZoomManager(TimeUnitStack timeUnitStack) {
    TimeUnitPair[] unitPairs = timeUnitStack.getTimeUnitPairs();
    myZoomStates = new ArrayList<ZoomState>(unitPairs.length);
    for (int i = 0; i < unitPairs.length; i++) {
      myZoomStates.add(new ZoomManager.ZoomState(unitPairs[i], i));
    }
  }

  public boolean canZoomIn() {
    return myZooming > 0;
  }

  public boolean canZoomOut() {
    return myZooming < myZoomStates.size() - 1 || myMaximumZoomStateReached == false;
  }

  public void zoomIn() {
    int oldValue = myZooming--;
    fireZoomingChanged(oldValue, myZooming);
  }

  public void zoomOut() {
    int oldValue = myZooming++;
    fireZoomingChanged(oldValue, myZooming);
  }

  public void addZoomListener(ZoomListener listener) {
    myListeners.add(listener);
    listener.zoomChanged(new ZoomEvent(this, getZoomState()));
  }

  public void removeZoomListener(ZoomListener listener) {
    myListeners.remove(listener);
  }

  private void fireZoomingChanged(int oldZoomValue, int newZoomValue) {
    ZoomEvent e = new ZoomEvent(this, getZoomState());
    for (int i = 0; i < myListeners.size(); i++) {
      ZoomListener nextListener = myListeners.get(i);
      nextListener.zoomChanged(e);
    }
  }

  /**
   * Find persistentName in the available ZoomStates or add new States until it
   * is found. Or the maximum allowed ZoomState is reached, in this case the
   * ZoomState is not changed.
   * 
   * @param persistentName
   *          is the ZoomState name to find
   */
  public void setZoomState(String persistentName) {
    for (int i = 0; i < myZoomStates.size() || myMaximumZoomStateReached == false; i++) {
      if (getZoomState(i).getPersistentName().equals(persistentName)) {
        myZooming = i;
        fireZoomingChanged(0, myZooming);
        break;
      }
    }
  }

  /** @return the selected ZoomState */
  public ZoomState getZoomState() {
    return getZoomState(myZooming);
  }

  public ZoomState getZoomState(int zoom) {
    while (zoom >= myZoomStates.size()) {
      // Zoom out even further by calculating the required ZoomState
      // The list of ZoomState is filled step by step until the desired
      // ZoomState is added (normally just the next step needs to be calculated)
      ZoomState lastZoomState = myZoomStates.get(myZoomStates.size() - 1);
      int bottomUnitWidth = (int) (lastZoomState.getBottomUnitWidth() * myZoomStep);
      if (bottomUnitWidth < 2) {
        // A bottomUnitWidth of 1 is the maximum allowed zoom,
        // since a width of 0 is not possible (and breaking stuff).
        myMaximumZoomStateReached = true;
      }
      myZoomStates.add(new ZoomState(lastZoomState.getTimeUnitPair(), bottomUnitWidth, zoom));
    }

    return myZoomStates.get(zoom);
  }
}
