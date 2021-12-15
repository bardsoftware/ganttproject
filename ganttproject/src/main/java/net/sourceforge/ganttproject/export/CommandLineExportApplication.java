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

import biz.ganttproject.LoggerApi;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.PluginPreferencesImpl;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.plugins.PluginManager;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.prefs.Preferences;
import org.w3c.util.DateParser;

import java.io.File;
import java.util.HashMap;
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

  private LoggerApi logger = GPLogger.create("Export");

  public boolean export(Args args, IGanttProject project, UIFacade uiFacade) {
    if (args.exporter == null) {
      return false;
    }
    for (Exporter exp: PluginManager.getExporters()) {
      var expWithFormat = exp.withFormat(args.exporter);
      if (expWithFormat != null) {
        return export(expWithFormat, args, project, uiFacade);
      }
    }
    return false;
  }

  private boolean export(Exporter exporter, Args args, IGanttProject project, UIFacade uiFacade) {
    logger.debug("Using exporter {}", new Object[]{exporter}, new HashMap<>());
    ConsoleUIFacade consoleUI = new ConsoleUIFacade(uiFacade);
    GPLogger.setUIFacade(consoleUI);
    // TODO: bring back task expanding
//    if (myArgs.expandTasks) {
//      for (Task t : project.getTaskManager().getTasks()) {
//        project.getUIFacade().getTaskTree().setExpanded(t, true);
//      }
//    }

    Job.getJobManager().setProgressProvider(new ConsoleProgressProvider());
    File outputFile = args.outputFile == null ? FileChooserPage.proposeOutputFile(project, exporter)
        : args.outputFile;

    Preferences prefs = new PluginPreferencesImpl(null, "");
    prefs.putInt("zoom", args.zooming);
    prefs.put(
        "exportRange",
        DateParser.getIsoDate(project.getTaskManager().getProjectStart()) + " "
            + DateParser.getIsoDate(project.getTaskManager().getProjectEnd()));
    prefs.putBoolean("commandLine", true);

    // If chart to export is defined, then add a string to prefs
    if (args.chart != null) {
      prefs.put("chart", args.chart);
    }

    // If stylesheet is defined, then add a string to prefs
    if (args.stylesheet != null) {
      prefs.put("stylesheet", args.stylesheet);
    }

    prefs.putBoolean("expandResources", args.expandResources);

    exporter.setContext(project, consoleUI, prefs);
    final CountDownLatch latch = new CountDownLatch(1);
    try {
      ExportFinalizationJob finalizationJob = exportedFiles -> latch.countDown();
      exporter.run(outputFile, finalizationJob);
      latch.await();
    } catch (Exception e) {
      consoleUI.showErrorDialog(e);
    }
    return true;
  }
}
