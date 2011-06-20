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
