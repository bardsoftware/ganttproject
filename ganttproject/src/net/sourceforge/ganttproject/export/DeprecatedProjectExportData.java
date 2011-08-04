package net.sourceforge.ganttproject.export;

import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.Chart;

/**
 * @deprecated This class is just a value object which is used for passing
 *             export parameters to export routines. It should be thrown away
 *             after export subsystem refactoring
 * @author bard
 * @created 13.09.2004
 */
public class DeprecatedProjectExportData {
    public final String myFilename;

    final Chart myGanttChart;

    final Chart myResourceChart;

    final GanttExportSettings myExportOptions;

    final String myXslFoScript;

    final IGanttProject myProject;

    public DeprecatedProjectExportData(IGanttProject project,
            final String myFilename, final Chart myGanttChart,
            final Chart myResourceChart,
            final GanttExportSettings myExportOptions,
            final String myXslFoScript) {
        super();
        this.myProject = project;
        this.myFilename = myFilename;
        this.myGanttChart = myGanttChart;
        this.myResourceChart = myResourceChart;
        this.myExportOptions = myExportOptions;
        this.myXslFoScript = myXslFoScript;
    }
}
