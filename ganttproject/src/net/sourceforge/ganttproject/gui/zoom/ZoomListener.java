/*
 * Created on 03.11.2004
 */
package net.sourceforge.ganttproject.gui.zoom;

import java.util.EventListener;

/**
 * @author bard
 */
public interface ZoomListener extends EventListener {
  void zoomChanged(ZoomEvent e);
}
