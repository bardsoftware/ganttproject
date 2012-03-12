/*
 * Created on 03.11.2004
 */
package net.sourceforge.ganttproject.gui.zoom;

import java.util.EventObject;

import net.sourceforge.ganttproject.gui.zoom.ZoomManager.ZoomState;

/**
 * @author bard
 */
public class ZoomEvent extends EventObject {
  private ZoomState myNewZoomState;

  ZoomEvent(ZoomManager manager, ZoomState newZoomState) {
    super(manager);
    myNewZoomState = newZoomState;
  }

  public ZoomState getNewZoomState() {
    return myNewZoomState;
  }
}
