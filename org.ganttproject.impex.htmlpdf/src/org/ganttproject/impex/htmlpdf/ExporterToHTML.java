/*
 * Created on 17.05.2005
 */
package org.ganttproject.impex.htmlpdf;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.export.ExportException;
import net.sourceforge.ganttproject.export.Exporter;
import net.sourceforge.ganttproject.export.TaskVisitor;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ProjectResource;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.util.FileUtil;

public class ExporterToHTML extends ExporterBase implements Exporter {
    private static final String PNG_FORMAT_NAME = "png";
    private AdvancedOptionGroup myAdvancedOptions = new AdvancedOptionGroup(new GPOption[0]);
    private EnumerationOption myStylesheetOption;
    private HTMLStylesheet mySelectedStylesheet;
    public String getFileTypeDescription() {
        return GanttLanguage.getInstance().getText("impex.html.description");
    }

    protected void setSelectedStylesheet(Stylesheet stylesheet) {
        mySelectedStylesheet = (HTMLStylesheet) stylesheet;
    }
    
    public GPOptionGroup[] getSecondaryOptions() {
        //return getGanttChart().getOptionGroups();
        return null;
    }    
    
    public String getFileNamePattern() {
        return "html";
    }

    protected Job[] createJobs(File outputFile, List resultFiles) {
        Job generateGanttChartJob = createGenerateGanttChartJob(outputFile, resultFiles);
        Job generateResourceChartJob = createGenerateResourceChartJob(outputFile, resultFiles);
        Job generatePagesJob = createGeneratePagesJob(outputFile, resultFiles);
        Job copyImagesJob = createCopyImagesJob(outputFile, resultFiles);
        return new Job[] {
                generateGanttChartJob, generateResourceChartJob, generatePagesJob, copyImagesJob
        };
    }
    
    private Job createGenerateGanttChartJob(final File outputFile, final List resultFiles) {
        Job result = new ExportJob("generate gantt chart") {
            protected IStatus run(IProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    Platform.getJobManager().cancel(ExporterBase.EXPORT_JOB_FAMILY);
                    return Status.CANCEL_STATUS;
                }
                try {
                    RenderedImage ganttChartImage = getGanttChart().getRenderedImage(new GanttExportSettings(true, true, true, true));
                    File ganttChartImageFile;
                    ganttChartImageFile = replaceExtension(outputFile, GANTT_CHART_FILE_EXTENSION);
                    ImageIO.write(ganttChartImage, PNG_FORMAT_NAME, ganttChartImageFile);
                    resultFiles.add(ganttChartImageFile);
                    monitor.worked(1);
                } catch (IOException e) {
                    getUIFacade().showErrorDialog(e);
                    this.cancel();
                    return Status.CANCEL_STATUS;
                } catch (OutOfMemoryError e) {
                    cancel();
                    ExporterToHTML.this.getUIFacade().showErrorDialog(new RuntimeException("Out of memory when creating Gantt chart image", e));
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        return result;
    }
    
    private Job createGenerateResourceChartJob(final File outputFile, final List resultFiles) {
        Job result = new ExportJob("Generate resource chart") {
            protected IStatus run(IProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    Platform.getJobManager().cancel(ExporterBase.EXPORT_JOB_FAMILY);
                    return Status.CANCEL_STATUS;
                }
                try {
                    RenderedImage resourceChartImage = getResourceChart().getRenderedImage(new GanttExportSettings(true, true, true, true));
                    File resourceChartImageFile = replaceExtension(outputFile, RESOURCE_CHART_FILE_EXTENSION);
                    ImageIO.write(resourceChartImage, PNG_FORMAT_NAME, resourceChartImageFile);
                    resultFiles.add(resourceChartImageFile);
                    monitor.worked(1);
                } catch (IOException e) {
                    getUIFacade().showErrorDialog(e);
                    this.cancel();
                    return Status.CANCEL_STATUS;
                } catch (OutOfMemoryError e) {
                    cancel();
                    ExporterToHTML.this.getUIFacade().showErrorDialog(new RuntimeException("Out of memory when creating resource chart image", e));
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        return result;
    }
    
    private Job createGeneratePagesJob(final File outputFile, final List resultFiles) {
        Job result = new ExportJob("Generate HTML pages") {

            protected IStatus run(IProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    Platform.getJobManager().cancel(ExporterBase.EXPORT_JOB_FAMILY);
                    return Status.CANCEL_STATUS;
                }
                try {
                    {
                        TransformerHandler handler = mySelectedStylesheet.createTitlePageHandler();
                        handler.setResult(new StreamResult(outputFile));
                        serialize(handler, outputFile);
                        resultFiles.add(outputFile);
                    }
                    //
                    {
                        TransformerHandler handler = mySelectedStylesheet.createTasksPageHandler();
                        File tasksPageFile = appendSuffixBeforeExtension(outputFile, "-tasks");
                        handler.setResult(new StreamResult(tasksPageFile));
                        serialize(handler, outputFile);
                        resultFiles.add(tasksPageFile);
                    }
                    {
                        TransformerHandler handler = mySelectedStylesheet.createGanttChartPageHandler();
                        File chartPageFile = appendSuffixBeforeExtension(outputFile, "-chart");
                        handler.setResult(new StreamResult(chartPageFile));
                        serialize(handler, outputFile);
                        resultFiles.add(chartPageFile);       
                    }
                    {
                        TransformerHandler handler = mySelectedStylesheet.createResourcesPageHandler();
                        File resourcesPageFile = appendSuffixBeforeExtension(outputFile, "-resources");
                        handler.setResult(new StreamResult(resourcesPageFile));
                        serialize(handler, outputFile);
                        resultFiles.add(resourcesPageFile);            
                        
                    }
                    monitor.worked(1);
                } catch (SAXException e) {
                    getUIFacade().showErrorDialog(e);
                    this.cancel();
                } catch (IOException e) {
                    this.cancel();
                    getUIFacade().showErrorDialog(e);
                } catch (OutOfMemoryError e) {
                    cancel();
                    ExporterToHTML.this.getUIFacade().showErrorDialog(new RuntimeException("Out of memory when running XSL transformation", e));
                    return Status.CANCEL_STATUS;
                } catch (ExportException e) {
                    cancel();
                    ExporterToHTML.this.getUIFacade().showErrorDialog(e);
				}
                finally {
                    
                }
                return Status.OK_STATUS;
            }
            
        };
        return result;
    }
    
    private Job createCopyImagesJob(final File outputFile, final List resultFiles) {
        Job result = new ExportJob("Copying images") {
            protected IStatus run(IProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    Platform.getJobManager().cancel(ExporterBase.EXPORT_JOB_FAMILY);
                    return Status.CANCEL_STATUS;
                }
                try {
                    File imagesDir = mySelectedStylesheet.getImagesDirectory();
                    if (imagesDir!=null && imagesDir.isDirectory() && imagesDir.exists()) {
                        File[] lof = imagesDir.listFiles();
                        if (lof.length != 0) {
                            File resultImagesDir = new File(outputFile.getParentFile(), imagesDir.getName());
                            if (resultImagesDir.mkdir()) {
                                for (int i = 0; i < lof.length; i++) {
                                    File nextInFile = lof[i];
                                    if (nextInFile.isDirectory()) {
                                    	continue;
                                    }
                                    File outFile = new File(resultImagesDir, nextInFile.getName());
                                    outFile.createNewFile();
                                    FileInputStream inStream = new FileInputStream(nextInFile);
                                    FileOutputStream outStream = new FileOutputStream(outFile);
                                    byte[] buffer = new byte[(int) nextInFile.length()];
                                    inStream.read(buffer);
                                    outStream.write(buffer);
                                }
                            }
                        }
                    }
                    monitor.worked(1);
                } catch (IOException e) {
                    getUIFacade().showErrorDialog(e);
                    this.cancel();
                    return Status.CANCEL_STATUS;
                }
                finally {
                    
                }
                return Status.OK_STATUS;
            }
        };
        return result;
    }
    
    private void serialize(TransformerHandler handler, File outputFile) throws SAXException, IOException, ExportException {
        String filenameWithoutExtension = getFilenameWithoutExtension(outputFile);
        handler.startDocument();
        AttributesImpl attrs = new AttributesImpl();
        
        writeViews(getUIFacade(), handler);
        startElement("ganttproject",attrs, handler);
        textElement("title", attrs, "GanttProject - " + filenameWithoutExtension, handler);
        //
        addAttribute("prefix", filenameWithoutExtension, attrs);
        startElement("links", attrs, handler);
        textElement("home", attrs, i18n("home"), handler);
        textElement("chart", attrs, i18n("gantt"), handler);
        textElement("tasks", attrs, i18n("task"), handler);
        textElement("resources", attrs, i18n("human"), handler);
        endElement("links", handler);
        //
        startElement("project", attrs, handler);
        addAttribute("title", i18n("project"), attrs);
        textElement("name", attrs, getProject().getProjectName(), handler);
        addAttribute("title", i18n("organization"), attrs);
        textElement("organization", attrs, getProject().getOrganization(), handler);
        addAttribute("title", i18n("webLink"), attrs);
        textElement("webLink", attrs, getProject().getWebLink(), handler);
        addAttribute("title", i18n("shortDescription"), attrs);
        textElement("description", attrs, getProject().getDescription(), handler);
        endElement("project", handler);
        //
        //TODO: [dbarashev, 10.09.2005] introduce output files grouping structure
        String ganttChartFileName = replaceExtension(outputFile,GANTT_CHART_FILE_EXTENSION).getName();
        textElement("chart", attrs, ganttChartFileName, handler);
        addAttribute("name", i18n("colName"), attrs);
        addAttribute("role", i18n("colRole"), attrs);
        addAttribute("mail", i18n("colMail"), attrs);
        addAttribute("phone", i18n("colPhone"), attrs);
        startElement("resources", attrs, handler);
        writeResources((HumanResourceManager) getProject().getHumanResourceManager(), handler);
        
        String resourceChartFileName = replaceExtension(outputFile, RESOURCE_CHART_FILE_EXTENSION).getName();
        addAttribute("path", resourceChartFileName, attrs);
        emptyElement("chart", attrs, handler);
        endElement("resources", handler);
        //
        addAttribute("name", i18n("name"), attrs);
        addAttribute("begin", i18n("start"), attrs);
        addAttribute("end", i18n("end"), attrs);
        addAttribute("milestone", i18n("meetingPoint"), attrs);
        addAttribute("progress", i18n("advancement"), attrs);
        addAttribute("assigned-to", i18n("assignTo"), attrs);
        addAttribute("notes", i18n("notesTask"), attrs);
        try {
            writeTasks(getProject().getTaskManager(), handler);
        } catch (Exception e) {
            throw new ExportException("Failed to write tasks", e);
        }
        //
        addAttribute("version", "Ganttproject ("+GanttProject.version + ")", attrs);
        addAttribute("date", GanttCalendar.getDateAndTime(), attrs);
        emptyElement("footer", attrs, handler);
        endElement("ganttproject", handler);
        handler.endDocument();
    }

    protected String getAssignedResourcesDelimiter() {
      return ", ";
    }

    public String proposeFileExtension() {
        return "html";
    }

    private class AdvancedOptionGroup extends GPOptionGroup implements GPOption {
        public AdvancedOptionGroup(GPOption[] options) {
            super("exporter.html.advanced", options);
        }

        public String getPersistentValue() {
            return null;
        }

        public void loadPersistentValue(String value) {
        }

        public boolean isChanged() {
            return false;
        }
        
    }

    public String[] getFileExtensions() {
        String s [] = {"html"};
        return s;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    protected String getStylesheetOptionID() {
        return "impex.html.stylesheet";
    }
    
    protected Stylesheet[] getStylesheets() {
        StylesheetFactoryImpl factory = new StylesheetFactoryImpl() {
            protected Stylesheet newStylesheet(URL resolvedUrl, String localizedName) {
                return new HTMLStylesheetImpl(resolvedUrl, localizedName);
            }
        };
        return (Stylesheet[]) factory.createStylesheets(HTMLStylesheet.class);
    }
   
    class HTMLStylesheetImpl extends StylesheetImpl implements HTMLStylesheet {
        HTMLStylesheetImpl(URL stylesheetURL, String localizedName) {
            super(stylesheetURL, localizedName);
        }
        public String getInputVersion() {
            return HTMLStylesheet.InputVersion.GP1X;
        }

        public TransformerHandler createTitlePageHandler() {
            try {
                URL titleUrl = new URL(getUrl(), "gantt.xsl");
                TransformerHandler result = createHandler(titleUrl.toString());
                return result;
                
            } catch (MalformedURLException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            	throw new RuntimeException(e);
            }
        }

        public TransformerHandler createTasksPageHandler() {
            try {
                URL tasksUrl = new URL(getUrl(), "gantt-tasks.xsl");
                TransformerHandler result = createHandler(tasksUrl.toString());
                return result;
                
            } catch (MalformedURLException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            	throw new RuntimeException(e);            	
            }
        }
        
        public TransformerHandler createGanttChartPageHandler() {
            try {
                URL tasksUrl = new URL(getUrl(), "gantt-chart.xsl");
                TransformerHandler result = createHandler(tasksUrl.toString());
                return result;
                
            } catch (MalformedURLException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            	throw new RuntimeException(e);
            }
        }
        public TransformerHandler createResourcesPageHandler() {
            try {
                URL tasksUrl = new URL(getUrl(), "gantt-resources.xsl");
                TransformerHandler result = createHandler(tasksUrl.toString());
                return result;
                
            } catch (MalformedURLException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            	throw new RuntimeException(e);
            }
        }
        public File getImagesDirectory() {
            try {
                URL imagesUrl = new URL(getUrl(), "images");
                assert imagesUrl!=null;
                File result = new File(imagesUrl.getPath());
                return result;
            } catch (MalformedURLException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            	throw new RuntimeException(e);
            }
        }
        
    }
    private static File replaceExtension(File f, String newExtension) throws IOException {
        File result = FileUtil.replaceExtension(f, newExtension);
        if (!result.exists()) {
            result.createNewFile();
        }
        return result;
    }
    
    private static File appendSuffixBeforeExtension(File f, String suffix) throws IOException {
        return FileUtil.appendSuffixBeforeExtension(f, suffix);
    }
    
    private static String getFilenameWithoutExtension(File f) {
        return FileUtil.getFilenameWithoutExtension(f);
    }
    
    private static final String GANTT_CHART_FILE_EXTENSION = "png";
    private static final String RESOURCE_CHART_FILE_EXTENSION = "res.png";
    
}
