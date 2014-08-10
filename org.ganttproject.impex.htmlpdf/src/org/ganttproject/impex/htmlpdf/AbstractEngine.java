/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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
package org.ganttproject.impex.htmlpdf;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.UIFacade;

import org.osgi.service.prefs.Preferences;

/**
 * Simple base class for the rendering engines.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class AbstractEngine {
  private IGanttProject myProject;
  private UIFacade myUiFacade;
  private Preferences myPreferences;

  public void setContext(IGanttProject project, UIFacade uiFacade, Preferences preferences) {
    myProject = project;
    myUiFacade = uiFacade;
    myPreferences = preferences;
  }

  protected UIFacade getUiFacade() {
    return myUiFacade;
  }

  protected IGanttProject getProject() {
    return myProject;
  }

  protected Preferences getPreferences() {
    return myPreferences;
  }

  protected Chart getGanttChart() {
    return myUiFacade.getGanttChart();
  }

  protected Chart getResourceChart() {
    return myUiFacade.getResourceChart();
  }

}
