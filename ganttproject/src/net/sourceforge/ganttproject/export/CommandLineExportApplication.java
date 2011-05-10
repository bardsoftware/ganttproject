package net.sourceforge.ganttproject.export;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.plugins.PluginManager;

import org.eclipse.core.runtime.jobs.Job;

public class CommandLineExportApplication {
    private final Map<String, Exporter> myFlag2exporter = new HashMap<String, Exporter>();

    public CommandLineExportApplication() {
        for (Exporter next : PluginManager.getExporters()) {
            List<String> nextExtensions = Arrays.asList(next.getCommandLineKeys());
            for (int j=0; j<nextExtensions.size(); j++) {
                myFlag2exporter.put("-" + nextExtensions.get(j), next);
            }
        }
    }

    public Collection<String> getCommandLineFlags() {
        return myFlag2exporter.keySet();
    }

    public boolean export(Map<String, List<String>> parsedArgs) {
        if (parsedArgs.isEmpty()) {
            return false;
        }

        List<String> values = new ArrayList<String>();
        Exporter exporter = findExporter(parsedArgs, values);
        if (exporter!=null && values.size()>0) {
            GanttProject project = new GanttProject(false);
            String inputFileName = values.get(0);

            project.openStartupDocument(inputFileName);
            ConsoleUIFacade consoleUI = new ConsoleUIFacade(project.getUIFacade());
            File inputFile = new File(inputFileName);
            if (false==inputFile.exists()) {
                consoleUI.showErrorDialog("File "+inputFileName+" does not exist.");
                return true;
            }
            if (false==inputFile.canRead()) {
                consoleUI.showErrorDialog("File "+inputFileName+" is not readable.");
                return true;
            }

            Job.getJobManager().setProgressProvider(null);
            File outputFile;
            if (values.size()>1) {
                outputFile = new File(values.get(1));
            }
            else {
                outputFile = FileChooserPage.proposeOutputFile(project, exporter);
            }
            System.err.println("[CommandLineExportApplication] export(): exporting with "+exporter);
            exporter.setContext(project, consoleUI, null);
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
        return false;
    }

    private Exporter findExporter(Map<String, List<String>> args, List<String> outputParams) {
        for (Iterator<Entry<String, Exporter>> exporters = myFlag2exporter
                .entrySet().iterator(); exporters.hasNext();) {
            Map.Entry<String, Exporter> nextEntry = exporters.next();
            String flag = nextEntry.getKey();
            if (args.containsKey(flag)) {
                outputParams.addAll(args.get(flag));
                return nextEntry.getValue();
            }
        }
        return null;
    }
}
