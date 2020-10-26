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
package net.sourceforge.ganttproject.action.zoom;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.zoom.ZoomEvent;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;

/**
 * @author bard
 */
public class ZoomInAction extends GPAction implements ZoomListener {
  private final ZoomManager myZoomManager;

  public ZoomInAction(ZoomManager zoomManager) {
    super("zoom.in");
    myZoomManager = zoomManager;
    myZoomManager.addZoomListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    if (myZoomManager.canZoomIn()) {
      myZoomManager.zoomIn();
    }
  }

  @Override
  public void zoomChanged(ZoomEvent e) {
    setEnabled(myZoomManager.canZoomIn());
  }

  @Override
  protected String getIconFilePrefix() {
    return "zoomp_";
  }
}
