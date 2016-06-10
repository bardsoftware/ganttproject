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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.PluginPreferencesImpl;
import net.sourceforge.ganttproject.plugins.PluginManager;
import net.sourceforge.ganttproject.task.Task;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.prefs.Preferences;
import org.w3c.util.DateParser;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class CommandLineExportApplication {
  public static class Args {
    @Parameter(names = "-export", description = "Export format")
    public String exporter;

    @Parameter(names = "-stylesheet", description = "Stylesheet used for export")
    public String stylesheet;

    @Parameter(names = "-chart", description = "Chart to export (resource or gantt)")
    public String chart;

    @Parameter(names = "-zoom", description = "Zoom scale to use in the exported charts")
    public Integer zooming = 3;

    @Parameter(names = { "-o", "-out" }, description = "Output file name", converter = FileConverter.class)
    public File outputFile;

    @Parameter(names = "-expand-resources", description = "Expand resource nodes on the resource load chart")
    public boolean expandResources = false;

    @Parameter(names = "-expand-tasks", description = "Expand all tasks nodes on the Gantt chart", arity = 1)
    public boolean expandTasks = true;

  }

  private final Map<String, Exporter> myFlag2exporter = new HashMap<String, Exporter>();

  private final Args myArgs = new Args();

  public CommandLineExportApplication() {
    for (Exporter exporter : PluginManager.getExporters()) {
      List<String> keys = Arrays.asList(exporter.getCommandLineKeys());
      for (String key : keys) {
        myFlag2exporter.put(key, exporter);
      }
    }
  }

  public Collection<String> getCommandLineFlags() {
    return myFlag2exporter.keySet();
  }

  public Args getArguments() {
    return myArgs;
  }

  public boolean export(GanttProject.Args mainArgs) {
    if (myArgs.exporter == null || mainArgs.file == null || mainArgs.file.isEmpty()) {
      return false;
    }
    Exporter exporter = myFlag2exporter.get(myArgs.exporter);
    GPLogger.log("Using exporter=" + exporter);
    if (exporter == null) {
      return false;
    }
    GanttProject project = new GanttProject(false);
    ConsoleUIFacade consoleUI = new ConsoleUIFacade(project.getUIFacade());
    File inputFile = new File(mainArgs.file.get(0));
    if (false == inputFile.exists()) {
      consoleUI.showErrorDialog("File " + mainArgs.file + " does not exist.");
      return true;
    }
    if (false == inputFile.canRead()) {
      consoleUI.showErrorDialog("File " + mainArgs.file + " is not readable.");
      return true;
    }

    project.openStartupDocument(mainArgs.file.get(0));
    if (myArgs.expandTasks) {
      for (Task t : project.getTaskManager().getTasks()) {
        project.getUIFacade().getTaskTree().setExpanded(t, true);
      }
    }

    Job.getJobManager().setProgressProvider(null);
    File outputFile = myArgs.outputFile == null ? FileChooserPage.proposeOutputFile(project, exporter)
        : myArgs.outputFile;

    Preferences prefs = new PluginPreferencesImpl(null, "");
    prefs.putInt("zoom", myArgs.zooming);
    prefs.put(
        "exportRange",
        DateParser.getIsoDate(project.getTaskManager().getProjectStart()) + " "
            + DateParser.getIsoDate(project.getTaskManager().getProjectEnd()));
    prefs.putBoolean("commandLine", true);

    // If chart to export is defined, then add a string to prefs
    if (myArgs.chart != null) {
      prefs.put("chart", myArgs.chart);
    }

    // If stylesheet is defined, then add a string to prefs
    if (myArgs.stylesheet != null) {
      prefs.put("stylesheet", myArgs.stylesheet);
    }

    prefs.putBoolean("expandResources", myArgs.expandResources);

    exporter.setContext(project, consoleUI, prefs);
    final CountDownLatch latch = new CountDownLatch(1);
    try {
      ExportFinalizationJob finalizationJob = new ExportFinalizationJob() {
        @Override
        public void run(File[] exportedFiles) {
          latch.countDown();
        }
      };
      exporter.run(outputFile, finalizationJob);
      latch.await();
    } catch (Exception e) {
      consoleUI.showErrorDialog(e);
    }
    return true;
  }
}
