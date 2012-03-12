/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
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
@Deprecated
public class DeprecatedProjectExportData {
  public final String myFilename;

  final Chart myGanttChart;

  final Chart myResourceChart;

  final GanttExportSettings myExportOptions;

  final String myXslFoScript;

  final IGanttProject myProject;

  public DeprecatedProjectExportData(IGanttProject project, final String myFilename, final Chart myGanttChart,
      final Chart myResourceChart, final GanttExportSettings myExportOptions, final String myXslFoScript) {
    super();
    this.myProject = project;
    this.myFilename = myFilename;
    this.myGanttChart = myGanttChart;
    this.myResourceChart = myResourceChart;
    this.myExportOptions = myExportOptions;
    this.myXslFoScript = myXslFoScript;
  }
}
