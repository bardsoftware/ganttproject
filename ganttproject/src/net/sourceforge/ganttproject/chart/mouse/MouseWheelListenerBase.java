/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.chart.mouse;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.util.MouseUtil;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public abstract class MouseWheelListenerBase implements MouseWheelListener {
  private final ZoomManager myZoomManager;
  private final String myHScrollKeyStroke = GPAction.getKeyStrokeText("mouse.wheel.hscroll");
  private final String myVScrollKeyStroke = GPAction.getKeyStrokeText("mouse.wheel.vscroll");
  private final String myZoomKeyStroke = GPAction.getKeyStrokeText("mouse.wheel.zoom");

  public MouseWheelListenerBase(ZoomManager zoomManager) {
    myZoomManager = zoomManager;
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    String text = MouseUtil.toString(e);
    if (text.equals(myZoomKeyStroke)) {
      if (isRotationUp(e)) {
        fireZoomIn();
      } else {
        fireZoomOut();
      }
    } else if (text.equals(myHScrollKeyStroke) || text.equals(myVScrollKeyStroke)) {
      fireScroll(e);
    }
  }

  protected abstract void fireScroll(MouseWheelEvent e);

  private void fireZoomIn() {
    if (myZoomManager.canZoomIn()) {
      myZoomManager.zoomIn();
    }
  }

  private void fireZoomOut() {
    if (myZoomManager.canZoomOut()) {
      myZoomManager.zoomOut();
    }
  }

  private boolean isRotationUp(MouseWheelEvent e) {
    return e.getWheelRotation() < 0;
  }
}
