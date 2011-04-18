/*
 * Created on 26.09.2005
 */
package org.ganttproject.impex.htmlpdf;

import java.awt.Component;
import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.export.ExportException;
import net.sourceforge.ganttproject.export.ExportFinalizationJob;
import net.sourceforge.ganttproject.export.TaskVisitor;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.CustomColumnsValues;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.prefs.Preferences;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

abstract class ExporterBase {
    private IGanttProject myProject;
    private Chart myGanttChart;
    private GPOptionGroup myOptions;
    private Chart myResourceChart;
    private SAXTransformerFactory myFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    private UIFacade myUIFacade;
    private Preferences myRootPreferences;

    protected EnumerationOption createStylesheetOption(String optionID, final Stylesheet[] stylesheets) {
        String[] names = new String[stylesheets.length];
        for (int i = 0; i < stylesheets.length; i++) {
            names[i] = stylesheets[i].getLocalizedName();
        }
        EnumerationOption stylesheetOption = new DefaultEnumerationOption<Stylesheet>(optionID, names) {
            public void commit() {
                super.commit();
                String value = getValue();
                for (int i=0; i<stylesheets.length; i++) {
                    if (stylesheets[i].getLocalizedName().equals(value)) {
                        setSelectedStylesheet(stylesheets[i]);
                    }
                }
            }

        };
        return stylesheetOption;
    }

    protected abstract void setSelectedStylesheet(Stylesheet stylesheet);
    protected abstract Stylesheet[] getStylesheets();
    protected abstract String getStylesheetOptionID();
    static Object EXPORT_JOB_FAMILY = new String("Export job family");

    public ExporterBase() {
        final Stylesheet[] stylesheets = getStylesheets();
        EnumerationOption stylesheetOption= createStylesheetOption(getStylesheetOptionID(), stylesheets);
        stylesheetOption.lock();
        stylesheetOption.setValue(stylesheets[0].getLocalizedName());
        stylesheetOption.commit();
        myOptions = new GPOptionGroup("exporter.html", new GPOption[] {stylesheetOption});
        myOptions.setTitled(false);
    }

    public Component getCustomOptionsUI() {
        return null;
    }

    public void run(final File outputFile,
            final ExportFinalizationJob finalizationJob) throws Exception {
        final IJobManager jobManager = Job.getJobManager();
        final List<File> resultFiles = new ArrayList<File>();
        final Job[] jobs = createJobs(outputFile, resultFiles);
        final IProgressMonitor monitor = jobManager.createProgressGroup();
        final IProgressMonitor familyMonitor = new IProgressMonitor() {
            public void beginTask(String name, int totalWork) {
                monitor.beginTask(name, totalWork);
            }

            public void done() {
                monitor.done();
            }

            public void internalWorked(double work) {
                monitor.internalWorked(work);
            }

            public boolean isCanceled() {
                return monitor.isCanceled();
            }

            public void setCanceled(boolean value) {
                monitor.setCanceled(value);
                if (value) {
                    System.err.println("ExporterBase: canceling value="+EXPORT_JOB_FAMILY);
                    jobManager.cancel(EXPORT_JOB_FAMILY);
                }
            }

            public void setTaskName(String name) {
                monitor.setTaskName(name);
            }

            public void subTask(String name) {
                monitor.subTask(name);
            }

            public void worked(int work) {
                monitor.worked(work);
            }
        };
        Job starting = new Job("starting") {
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Running export", jobs.length);
                for (int i=0; i<jobs.length; i++) {
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    jobs[i].setProgressGroup(monitor, 1);
                    jobs[i].schedule();
                    try {
                        jobs[i].join();
                    } catch (InterruptedException e) {
                        myUIFacade.showErrorDialog(e);
                    }
                }
                Job finishing = new Job("finishing") {
                    protected IStatus run(IProgressMonitor monitor) {
                        monitor.done();
                        finalizationJob.run((File[]) resultFiles.toArray(new File[0]));
                        return Status.OK_STATUS;
                    }
                };
                finishing.setProgressGroup(monitor, 0);
                finishing.schedule();
                try {
                    finishing.join();
                } catch (InterruptedException e) {
                    myUIFacade.showErrorDialog(e);
                }
                return Status.OK_STATUS;
            }
        };
        starting.setProgressGroup(familyMonitor, 0);
        starting.schedule();
    }

    protected abstract Job[] createJobs(File outputFile, List<File> resultFiles);

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

    protected CustomColumnsStorage getCustomColumnStorage() {
        return getProject().getCustomColumnsStorage();
    }

    protected Chart getGanttChart() {
        return myGanttChart;
    }

    public GPOptionGroup getOptions() {
        return myOptions;
    }

    protected Chart getResourceChart() {
        return myResourceChart;
    }

    protected void startElement(String name, AttributesImpl attrs,
            TransformerHandler handler) throws SAXException {
        handler.startElement("", name, name, attrs);
        attrs.clear();
    }

    protected void startPrefixedElement(String name, AttributesImpl attrs,
            TransformerHandler handler) throws SAXException {
        handler.startElement("http://ganttproject.sf.net/", name,
                "ganttproject:" + name, attrs);
        attrs.clear();
    }

    protected void endElement(String name, TransformerHandler handler)
            throws SAXException {
        handler.endElement("", name, name);
    }

    protected void endPrefixedElement(String name, TransformerHandler handler)
            throws SAXException {
        handler.endElement("http://ganttproject.sf.net/", name, "ganttproject:"
                + name);
    }

    protected void addAttribute(String name, String value, AttributesImpl attrs) {
        if (value != null) {
            attrs.addAttribute("", name, name, "CDATA", value);
        } else {
            System.err.println("[GanttOptions] attribute '" + name + "' is null");
        }
    }

    protected void emptyElement(String name, AttributesImpl attrs,
            TransformerHandler handler) throws SAXException {
        startElement(name, attrs, handler);
        endElement(name, handler);
        attrs.clear();
    }

    protected void textElement(String name, AttributesImpl attrs, String text, TransformerHandler handler) throws SAXException {
        if (text != null) {
            startElement(name, attrs, handler);
            handler.startCDATA();
            handler.characters(text.toCharArray(), 0, text.length());
            handler.endCDATA();
            endElement(name, handler);
            attrs.clear();
        }
    }

    protected SAXTransformerFactory getTransformerFactory() {
        return myFactory;
    }

    protected TransformerHandler createHandler(String xsltPath) {
        try {
            TransformerHandler result = getTransformerFactory().newTransformerHandler(new StreamSource(xsltPath));
            Transformer transformer = result.getTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "4");
            return result;

        } catch (TransformerConfigurationException e) {
            myUIFacade.showErrorDialog(e);
            throw new RuntimeException(e);
        }
    }

    protected void addAttribute(String name, int value, AttributesImpl attrs) {
        addAttribute(name, String.valueOf(value), attrs);
    }


    protected String i18n(String key) {
        String text = GanttLanguage.getInstance().getText(key);
        return GanttLanguage.getInstance().correctLabel(text);
    }

    protected void writeColumns(TableHeaderUIFacade visibleFields, TransformerHandler handler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        int totalWidth = 0;
        for (int i=0; i<visibleFields.getSize(); i++) {
            if (visibleFields.getField(i).isVisible()) {
                totalWidth += visibleFields.getField(i).getWidth();
            }
        }
        for (int i=0; i<visibleFields.getSize(); i++) {
            TableHeaderUIFacade.Column field = visibleFields.getField(i);
            if (field.isVisible()) {
                addAttribute("id", field.getID(), attrs);
                addAttribute("name", field.getName(), attrs);
                addAttribute("width", field.getWidth()*100/totalWidth, attrs);
                emptyElement("field", attrs, handler);
            }
        }
    }
    protected void writeViews(UIFacade facade, TransformerHandler handler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        addAttribute("id", "task-table", attrs);
        startElement("view", attrs, handler);
        writeColumns(facade.getTaskTree().getVisibleFields(), handler);
        endElement("view", handler);

        addAttribute("id", "resource-table", attrs);
        startElement("view", attrs, handler);
        writeColumns(facade.getResourceTree().getVisibleFields(), handler);

        endElement("view", handler);
    }


    protected void writeTasks(TaskManager taskManager,
            final TransformerHandler handler) throws ExportException,
            SAXException {
        AttributesImpl attrs = new AttributesImpl();
        addAttribute("xslfo-path", "", attrs);
        addAttribute("title", i18n("tasksList"), attrs);
        addAttribute("name", i18n("name"), attrs);
        addAttribute("begin", i18n("start"), attrs);
        addAttribute("end", i18n("end"), attrs);
        addAttribute("milestone", i18n("meetingPoint"), attrs);
        addAttribute("progress", "%", attrs);
        addAttribute("assigned-to", i18n("human"), attrs);
        addAttribute("notes", i18n("notes"), attrs);
        addAttribute("duration", i18n("duration"), attrs);
        startPrefixedElement("tasks", attrs, handler);
        TaskVisitor visitor = new TaskVisitor() {
            AttributesImpl myAttrs = new AttributesImpl();
            protected String serializeTask(Task t, int depth) throws Exception {
                addAttribute("depth", depth, myAttrs);
                startPrefixedElement("task", myAttrs, handler);
                {
                    addAttribute("id", "tpd1", myAttrs);
                    textElement("priority", myAttrs, i18n(t.getPriority().getI18nKey()), handler);
                }

                addAttribute("id", "tpd3", myAttrs);
                textElement("name", myAttrs, t.getName(), handler);

                addAttribute("id", "tpd4", myAttrs);
                textElement("begin", myAttrs, t.getStart().toString(), handler);

                addAttribute("id", "tpd5", myAttrs);
                textElement("end", myAttrs, t.getEnd().toString(), handler);
                textElement("milestone", myAttrs, Boolean.valueOf(
                        t.isMilestone()).toString(), handler);

                addAttribute("id", "tpd7", myAttrs);
                textElement("progress", myAttrs, String.valueOf(t
                        .getCompletionPercentage()), handler);

                addAttribute("id", "tpd6", myAttrs);
                textElement("duration", myAttrs, String.valueOf(t.getDuration().getLength()), handler);

                final List<Document> attachments = t.getAttachments();
                for (int i = 0; i < attachments.size(); i++) {
                    Document nextAttachment = attachments.get(i);
                    URI nextUri = nextAttachment.getURI();
                    if (nextUri != null) {
                        String strUri = URLDecoder.decode(nextUri.toString(), "utf-8");
                        if (strUri.startsWith("file:")) {
                            if (strUri.endsWith("/")) {
                                strUri = strUri.replaceAll("/+$", "");
                            }
                            int lastSlash = strUri.lastIndexOf('/');
                            if (lastSlash >= 0) {
                                addAttribute("display-name", strUri.substring(lastSlash+1), myAttrs);
                            }
                        }
                        textElement("attachment", myAttrs, strUri, handler);
                    } else {
                        textElement("attachment", myAttrs, nextAttachment.getPath(), handler);
                    }
                }
                {
                    HumanResource coordinator = t.getAssignmentCollection().getCoordinator();
                    if (coordinator!=null) {
                        addAttribute("id", "tpd8", myAttrs);
                        textElement("coordinator", myAttrs, coordinator.getName(), handler);
                    }
                }
                StringBuffer usersS = new StringBuffer();
                ResourceAssignment[] assignments = t.getAssignments();
                if (assignments.length > 0) {
                    for (int j = 0; j < assignments.length; j++) {
                        addAttribute("resource-id", assignments[j].getResource().getId(), myAttrs);
                        emptyElement("assigned-resource", myAttrs, handler);
                        usersS.append(assignments[j].getResource().getName());
                        if (j<assignments.length-1) {
                          usersS.append(getAssignedResourcesDelimiter());
                        }
                    }
                }

                addAttribute("id", "tpdResources", myAttrs);
                textElement("assigned-to", myAttrs, usersS.toString(), handler);
                if (t.getNotes()!=null && t.getNotes().length()>0) {
                    textElement("notes", myAttrs, t.getNotes(), handler);
                }
                if (t.getColor()!=null) {
                    textElement("color", myAttrs, getHexaColor(t.getColor()),
                            handler);
                }
                {
                    AttributesImpl attrs = new AttributesImpl();
                    CustomColumnsValues customValues = t.getCustomValues();
                    for (Iterator<CustomColumn> it = getCustomColumnStorage()
                            .getCustomColums().iterator(); it.hasNext();) {
                        CustomColumn nextColumn = it.next();
                        Object value = customValues.getValue(nextColumn.getName());
                        String valueAsString = value==null ? "" : value.toString();
                        addAttribute("id", nextColumn.getId(), attrs);
                        textElement("custom-field", attrs, valueAsString, handler);
                    }
                }
                endPrefixedElement("task", handler);
                return "";
            }
        };
        try {
            visitor.visit(taskManager);
        } catch (Exception e) {
            throw new ExportException("Failed to write tasks", e);
        }
        endPrefixedElement("tasks", handler);
    }

    protected String getAssignedResourcesDelimiter() {
      return " ";
    }

    protected void writeResources(HumanResourceManager resourceManager,
            TransformerHandler handler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        addAttribute("title", i18n("resourcesList"), attrs);
        addAttribute("name", i18n("colName"), attrs);
        addAttribute("role", i18n("colRole"), attrs);
        addAttribute("mail", i18n("colMail"), attrs);
        addAttribute("phone", i18n("colPhone"), attrs);
        startPrefixedElement("resources", attrs, handler);
        {
            List<HumanResource> resources = resourceManager.getResources();

            // String
            // []function=RoleManager.Access.getInstance().getRoleNames();
            for (int i = 0; i < resources.size(); i++) {
                HumanResource p = resources.get(i);
                addAttribute("id", p.getId(), attrs);
                startPrefixedElement("resource", attrs, handler);
                addAttribute("id", "0", attrs);
                textElement("name", attrs, p.getName(), handler);
                addAttribute("id", "1", attrs);
                textElement("role", attrs, p.getRole().getName(), handler);
                addAttribute("id", "2", attrs);
                textElement("mail", attrs, p.getMail(), handler);
                addAttribute("id", "3", attrs);
                textElement("phone", attrs, p.getPhone(), handler);

                List<CustomProperty> customFields = p.getCustomProperties();
                for (int j=0; j<customFields.size(); j++) {
                    CustomProperty nextProperty = customFields.get(j);
                    addAttribute("id", nextProperty.getDefinition().getID(), attrs);
                    String value = nextProperty.getValueAsString();
                    textElement("custom-field", attrs, value, handler);
                }
                endPrefixedElement("resource", handler);
            }
        }
        endPrefixedElement("resources", handler);
    }

    protected static String getHexaColor(java.awt.Color color) {
        StringBuffer out = new StringBuffer();
        out.append("#");
        if (color.getRed() <= 15) {
            out.append("0");
        }
        out.append(Integer.toHexString(color.getRed()));
        if (color.getGreen() <= 15) {
            out.append("0");
        }
        out.append(Integer.toHexString(color.getGreen()));
        if (color.getBlue() <= 15) {
            out.append("0");
        }
        out.append(Integer.toHexString(color.getBlue()));

        return out.toString();
    }
}
