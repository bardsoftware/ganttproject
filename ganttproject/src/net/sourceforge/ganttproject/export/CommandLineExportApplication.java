package net.sourceforge.ganttproject.export;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.PluginPreferencesImpl;
import net.sourceforge.ganttproject.plugins.PluginManager;

import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.prefs.Preferences;
import org.w3c.util.DateParser;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

public class CommandLineExportApplication {
    public static class Args {
        @Parameter(names = "-export", description = "Export format")
        public String exporter;

        @Parameter(names = "-zoom", description = "Zoom scale to use in the exported charts")
        public Integer zooming = 3;

        @Parameter(names = {"-o", "-out"}, description = "Output file name", converter = FileConverter.class)
        public File outputFile;
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
        if (exporter == null) {
            return false;
        }

        GanttProject project = new GanttProject(false);
        project.openStartupDocument(mainArgs.file.get(0));
        ConsoleUIFacade consoleUI = new ConsoleUIFacade(project.getUIFacade());
        File inputFile = new File(mainArgs.file.get(0));
        if (false==inputFile.exists()) {
            consoleUI.showErrorDialog("File "+mainArgs.file+" does not exist.");
            return true;
        }
        if (false==inputFile.canRead()) {
            consoleUI.showErrorDialog("File "+mainArgs.file+" is not readable.");
            return true;
        }

        Job.getJobManager().setProgressProvider(null);
        File outputFile = myArgs.outputFile == null
            ?  FileChooserPage.proposeOutputFile(project, exporter) : myArgs.outputFile;

        System.err.println("[CommandLineExportApplication] export(): exporting with "+exporter);
        Preferences prefs = new PluginPreferencesImpl(null, "");
        prefs.putInt("zoom", myArgs.zooming);
        prefs.put("exportRange",
            DateParser.getIsoDate(project.getTaskManager().getProjectStart()) + " "
            + DateParser.getIsoDate(project.getTaskManager().getProjectEnd()));
        exporter.setContext(project, consoleUI, prefs);
        if (exporter instanceof ExportFileWizardImpl.LegacyOptionsClient) {
            ((ExportFileWizardImpl.LegacyOptionsClient)exporter).setOptions(project.getGanttOptions());
        }
        try {
            ExportFinalizationJob finalizationJob = new ExportFinalizationJob() {
                public void run(File[] exportedFiles) {
                    System.exit(0);
                }
            };
            exporter.run(outputFile, finalizationJob);
        } catch (Exception e) {
            consoleUI.showErrorDialog(e);
        }
        return true;
    }
}
