/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.export;

import org.osgi.service.prefs.Preferences;
import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager.ZoomState;

public abstract class AbstractExporter implements Exporter {
    private IGanttProject myProject;
    private Chart myGanttChart;
    private Chart myResourceChart;
    private UIFacade myUIFacade;
    private Preferences myRootPreferences;

    public void setContext(IGanttProject project, UIFacade uiFacade, Preferences prefs) {
        myGanttChart= uiFacade.getGanttChart();
        myResourceChart = uiFacade.getResourceChart();
        myProject = project;
        myUIFacade = uiFacade;
        myRootPreferences = prefs;
    }

    protected UIFacade getUIFacade() {
        return myUIFacade;
    }

    protected IGanttProject getProject() {
        return myProject;
    }

    protected Preferences getPreferences() {
        return myRootPreferences;
    }

    protected Chart getGanttChart() {
        return myGanttChart;
    }

    protected Chart getResourceChart() {
        return myResourceChart;
    }

    protected GanttExportSettings createExportSettings() {
        GanttExportSettings result = new GanttExportSettings();
        if (myRootPreferences != null) {
            int zoomLevel = myRootPreferences.getInt("zoom", -1);
            ZoomState zoomState = zoomLevel < 0 ?
                myUIFacade.getZoomManager().getZoomState() : myUIFacade.getZoomManager().getZoomState(zoomLevel);
            result.setZoomLevel(zoomState);

            String exportRange = myRootPreferences.get("exportRange", null);
            if (exportRange != null) {
                String[] rangeBounds = exportRange.split(" ");

                try {
                    result.setStartDate(DateParser.parse(rangeBounds[0]));
                    result.setEndDate(DateParser.parse(rangeBounds[1]));
                } catch (InvalidDateException e) {
                    myUIFacade.logErrorMessage(e);
                }
                result.setWidth(-1);
            }
        }
        return result;
    }
}
