/*
GanttProject is an opensource project management tool.
Copyright (C) 2011-2012 GanttProject Team

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
package net.sourceforge.ganttproject.export;

import biz.ganttproject.app.JobMonitorModel;
import biz.ganttproject.core.option.*;
import kotlinx.coroutines.CoroutineScope;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.eclipse.core.runtime.Status;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.prefs.Preferences;
import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ExporterBase implements Exporter {
  private IGanttProject myProject;
  private Chart myGanttChart;
  private Chart myResourceChart;
  private UIFacade myUIFacade;
  private Preferences myRootPreferences;
  private ObservableDateOption myExportRangeStart;
  private ObservableDateOption myExportRangeEnd;

  protected static final GanttLanguage language = GanttLanguage.getInstance();

  @Override
  public void setContext(IGanttProject project, UIFacade uiFacade, Preferences prefs) {
    myGanttChart = uiFacade.getGanttChart();
    myResourceChart = uiFacade.getResourceChart();
    myProject = project;
    myUIFacade = uiFacade;
    myRootPreferences = prefs;
    Preferences prefNode = prefs.node("/instance/net.sourceforge.ganttproject/export");
    myExportRangeStart = new ObservableDateOption("export.range.start", myGanttChart.getStartDate());
    myExportRangeStart.loadPersistentValue(prefNode.get(
        "export-range-start", DateParser.getIsoDate(myGanttChart.getStartDate())));
    myExportRangeStart.addChangeValueListener(event -> {
      prefNode.put("export-range-start", myExportRangeStart.getPersistentValue());
    });
    myExportRangeStart.setValueValidator(date -> {
      if (date.after(myExportRangeEnd.getValue())) {
        return new kotlin.Pair(false, "Start date > end date");
      } else {
        return new kotlin.Pair(true, "");
      }
    });
    myExportRangeEnd = new ObservableDateOption("export.range.end", myGanttChart.getEndDate());
    myExportRangeEnd.loadPersistentValue(prefNode.get(
        "export-range-end", DateParser.getIsoDate(myGanttChart.getEndDate())));
    myExportRangeEnd.setValueValidator(date -> {
      if (date.before(myExportRangeStart.getValue())) {
        return new kotlin.Pair(false, "Start date > end date");
      } else {
        return new kotlin.Pair(true, "");
      }
    });
    myExportRangeEnd.addChangeValueListener(event -> {
      prefNode.put("export-range-end", myExportRangeEnd.getPersistentValue());
    });
  }

  protected DateOption getExportRangeStartOption() {
    return myExportRangeStart;
  }

  protected DateOption getExportRangeEndOption() {
    return myExportRangeEnd;
  }

  protected GPOptionGroup createExportRangeOptionGroup() {
    return new GPOptionGroup("export.range", getExportRangeStartOption(), getExportRangeEndOption());
  }

  public UIFacade getUIFacade() {
    return myUIFacade;
  }

  public IGanttProject getProject() {
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

  @Override
  public @Nullable Exporter withFormat(String format) {
    if (Arrays.asList(getFileExtensions()).contains(format)) {
      setFormat(format);
      return this;
    } else {
      return null;
    }
  }

  protected void setFormat(String format) {}

  //  @Override
//  public String[] getCommandLineKeys() {
//    // By default use the same
//    return getFileExtensions();
//  }

  public GanttExportSettings createExportSettings() {
    GanttExportSettings result = new GanttExportSettings();
    if (myRootPreferences != null) {
      String exportRange = myRootPreferences.get("exportRange", null);
      if (exportRange == null) {
        result.setStartDate(myExportRangeStart.getValue());
        result.setEndDate(myExportRangeEnd.getValue());
      } else {
        String[] rangeBounds = exportRange.split(" ");

        try {
          result.setStartDate(DateParser.parse(rangeBounds[0]));
          result.setEndDate(DateParser.parse(rangeBounds[1]));
        } catch (InvalidDateException e) {
          GPLogger.log(e);
        }
      }
      if (result.getStartDate().after(result.getEndDate())) {
        GPLogger.log(new ValidationException("In the export range the start date=" + result.getStartDate() + " is after the end date=" + result.getEndDate()));
      }
      result.setCommandLineMode(myRootPreferences.getBoolean("commandLine", false));
      if (myRootPreferences.getBoolean("expandResources", false)) {
        result.setExpandedResources("");
      }
    }
    return result;
  }

  @Override
  public void run(final CoroutineScope coroutineScope, final File outputFile, final ExportFinalizationJob finalizationJob, JobMonitorModel jobMonitor) throws Exception {
    final List<File> resultFiles = new ArrayList<>();

    var jobs = new ArrayList<>(Arrays.asList(createJobs(outputFile, resultFiles)));
    jobs.add(new JavaExporterJob("Finalizing", () -> {
      finalizationJob.run(resultFiles.toArray(new File[0]));
      return Status.OK_STATUS;
    }));
    ExporterBackgroundJobsKt.export(coroutineScope, jobs, jobMonitor);
  }

  /** @return a list with {@link ExporterJob}s required to actually export the current format */
  protected abstract ExporterJob[] createJobs(File outputFile, List<File> resultFiles);

}
