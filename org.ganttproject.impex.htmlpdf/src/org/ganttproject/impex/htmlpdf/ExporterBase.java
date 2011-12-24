/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 Alexandre Thomas, Dmitry Barashev, GanttProject team

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
package org.ganttproject.impex.htmlpdf;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.export.AbstractExporter;
import net.sourceforge.ganttproject.export.ExportFinalizationJob;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.prefs.Preferences;

public abstract class ExporterBase extends AbstractExporter {

    public abstract static class ExporterJob {
        private final String myName;

        protected ExporterJob(String name) {
            myName = name;
        }

        String getName() {
            return myName;
        }

        protected abstract IStatus run();
    }

    private GPOptionGroup myOptions;

    protected EnumerationOption createStylesheetOption(String optionID, final List<Stylesheet> stylesheets) {
        final List<String> names = new ArrayList<String>();
        for (Stylesheet s : stylesheets) {
            names.add(s.getLocalizedName());
        }
        EnumerationOption stylesheetOption = new DefaultEnumerationOption<Stylesheet>(optionID, names) {
            @Override
            public void commit() {
                super.commit();
                String value = getValue();
                int index = names.indexOf(value);
                if (index >= 0) {
                    setSelectedStylesheet(stylesheets.get(index));
                }
            }
        };
        return stylesheetOption;
    }

    @Override
    public abstract String[] getFileExtensions();

    protected abstract List<Stylesheet> getStylesheets();

    protected abstract void setSelectedStylesheet(Stylesheet stylesheet);

    protected abstract String getStylesheetOptionID();

    static Object EXPORT_JOB_FAMILY = new String("Export job family");

    public ExporterBase() {
    }

    @Override
    public Component getCustomOptionsUI() {
        return null;
    }

    @Override
    public String[] getCommandLineKeys() {
        return getFileExtensions();
    }

    @Override
    public void setContext(IGanttProject project, UIFacade uiFacade, Preferences prefs) {
        super.setContext(project, uiFacade, prefs);
        createStylesheetOption(getStylesheets());
    }

    @Override
    public String getFileNamePattern() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFileTypeDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<GPOptionGroup> getSecondaryOptions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String proposeFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    private void createStylesheetOption(List<Stylesheet> stylesheets) {
        EnumerationOption stylesheetOption = createStylesheetOption(getStylesheetOptionID(), stylesheets);
        stylesheetOption.setValue(stylesheets.get(0).getLocalizedName());
        myOptions = new GPOptionGroup("exporter.html", new GPOption[] { stylesheetOption });
        myOptions.setTitled(false);
    }

    @Override
    public void run(final File outputFile, final ExportFinalizationJob finalizationJob) throws Exception {
        final IJobManager jobManager = Job.getJobManager();
        final List<File> resultFiles = new ArrayList<File>();
        final List<ExporterJob> jobs = new ArrayList<ExporterJob>(Arrays.asList(createJobs(outputFile, resultFiles)));
        jobs.add(new ExporterJob("Finalizing") {
            @Override
            protected IStatus run() {
                finalizationJob.run(resultFiles.toArray(new File[0]));
                return Status.OK_STATUS;
            }
        });
        final IProgressMonitor monitor = jobManager.createProgressGroup();
        Job driverJob = new Job("Running export") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Running export", jobs.size());
                for (ExporterJob job : jobs) {
                    if (monitor.isCanceled()) {
                        jobManager.cancel(EXPORT_JOB_FAMILY);
                        return Status.CANCEL_STATUS;
                    }
                    monitor.subTask(job.getName());
                    final IStatus state;
                    try {
                        state = job.run();
                    } catch (Throwable e) {
                        GPLogger.log(new RuntimeException("Export failure. Failed subtask: " + job.getName(), e));
                        monitor.setCanceled(true);
                        continue;
                    }

                    if (state.isOK() == false) {
                        GPLogger.log(new RuntimeException("Export failure. Failed subtask: " + job.getName(), state.getException()));
                        monitor.setCanceled(true);
                        continue;
                    }
                    // Sub task for export is finished without problems
                    // So, updated the total amount of work with the current
                    // work performed
                    monitor.worked(1);
                    // and remove the sub task description
                    // (convenient for debugging to know the sub task is
                    // finished properly)
                    monitor.subTask(null);
                }
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        driverJob.setProgressGroup(monitor, 0);
        driverJob.schedule();
    }

    protected abstract ExporterJob[] createJobs(File outputFile, List<File> resultFiles);

    @Override
    public GPOptionGroup getOptions() {
        return myOptions;
    }
}
