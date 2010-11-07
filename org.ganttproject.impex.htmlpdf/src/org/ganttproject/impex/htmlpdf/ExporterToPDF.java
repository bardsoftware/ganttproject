/*
 * Created on 18.09.2005
 */
package org.ganttproject.impex.htmlpdf;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.export.ExportException;
import net.sourceforge.ganttproject.export.Exporter;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.apache.fop.apps.Driver;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Options;
import org.apache.fop.image.FopImageFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.ganttproject.impex.htmlpdf.fonts.FontRecord;
import org.ganttproject.impex.htmlpdf.fonts.FontTriplet;
import org.ganttproject.impex.htmlpdf.fonts.JDKFontLocator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class ExporterToPDF extends ExporterBase implements Exporter {
    private static final String JPG_FORMAT_NAME = "jpg";
    private PDFStylesheet myStylesheet;

    public String getFileTypeDescription() {
        return GanttLanguage.getInstance().getText("impex.pdf.description");
    }

    public GPOptionGroup[] getSecondaryOptions() {
        return null;
    }

    public String getFileNamePattern() {
        return "pdf";
    }

    public String proposeFileExtension() {
        return "pdf";
    }

    public String[] getFileExtensions() {
        return new String[]{"pdf"};
    }

    protected Job[] createJobs(File outputFile, List<File> resultFiles) {
        ExportState state = new ExportState(outputFile);
        Job generateGanttChart = createGenerateGanttChartJob(state);
        Job generateResourceChart = createGenerateResourcechartJob(state);
        Job initializeFOP = createFOPInitializationJob(state);
        Job runTransormation = createTransformationJob(state, resultFiles);
        return new Job[]{generateGanttChart, generateResourceChart,
                initializeFOP, runTransormation};
    }

    private Job createGenerateGanttChartJob(final ExportState state) {
        Job result = new ExportJob("generate gantt chart") {
            protected IStatus run(IProgressMonitor monitor) {
            	if (monitor.isCanceled()) {
            		Platform.getJobManager().cancel(ExporterBase.EXPORT_JOB_FAMILY);
            		return Status.CANCEL_STATUS;
            	}
                try {
                    RenderedImage ganttChartImage = getGanttChart().getRenderedImage(
                            new GanttExportSettings(true, true, true, true));
                    state.ganttChartImageFile = File.createTempFile(
                            "ganttchart", ".jpg");
                    ImageIO.write(ganttChartImage, JPG_FORMAT_NAME,
                            state.ganttChartImageFile);
                    monitor.worked(1);
                } catch (Exception e) {
                	cancel();
                	ExporterToPDF.this.getUIFacade().showErrorDialog(e);
                    return Status.CANCEL_STATUS;

                } catch (OutOfMemoryError e) {
                	cancel();
                	ExporterToPDF.this.getUIFacade().showErrorDialog(e);
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }


        };
        return result;

    }

    private Job createGenerateResourcechartJob(final ExportState state) {
        Job result = new ExportJob("Generate resource chart") {
            protected IStatus run(IProgressMonitor monitor) {
            	if (monitor.isCanceled()) {
            		Platform.getJobManager().cancel(ExporterBase.EXPORT_JOB_FAMILY);
            		return Status.CANCEL_STATUS;
            	}
                try {
                    RenderedImage resourceChartImage = getResourceChart().getRenderedImage(
                            new GanttExportSettings(true, true, true, true));
                    File outputFile = File.createTempFile("resourcechart",
                            ".jpg");
                    state.resourceChartImageFile = outputFile;
                    ImageIO.write(resourceChartImage, JPG_FORMAT_NAME,
                            outputFile);
                    monitor.worked(1);
                } catch (Exception e) {
                	cancel();
                	ExporterToPDF.this.getUIFacade().showErrorDialog(e);
                    return Status.CANCEL_STATUS;

                } catch (OutOfMemoryError e) {
                	cancel();
                	ExporterToPDF.this.getUIFacade().showErrorDialog(e);
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        return result;
    }

    private Job createFOPInitializationJob(final ExportState state) {
        Job result = new ExportJob("Initializing FOP") {
            protected IStatus run(IProgressMonitor monitor) {
            	if (monitor.isCanceled()) {
            		Platform.getJobManager().cancel(ExporterBase.EXPORT_JOB_FAMILY);
            		return Status.CANCEL_STATUS;
            	}
                try {
                    Driver driver = new Driver();
                    driver.setRenderer(Driver.RENDER_PDF);
                    createOptions();
                    FopImageFactory.resetCache();
                    state.driver = driver;
                    monitor.worked(1);
                	//throw new RuntimeException("Moooo!!!!!");
                } catch (Exception e) {
                	cancel();
                	ExporterToPDF.this.getUIFacade().showErrorDialog(e);
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        return result;
    }

    private Job createTransformationJob(final ExportState state, final List<File> resultFiles) {
        Job result = new ExportJob("Generating PDF") {
            protected IStatus run(IProgressMonitor monitor) {
            	if (monitor.isCanceled()) {
            		Platform.getJobManager().cancel(ExporterBase.EXPORT_JOB_FAMILY);
            		return Status.CANCEL_STATUS;
            	}
                assert myStylesheet!=null;
                OutputStream out = null;
                try {
                    out = new FileOutputStream(state.outputFile);
                    state.driver.setOutputStream(out);
                    TransformerHandler stylesheetHandler = createHandler(myStylesheet
                            .getUrl().toString());
//                     SAXTransformerFactory factory = getTransformerFactory();
//                     TransformerHandler stylesheetHandler =
//                     factory.newTransformerHandler();
//                     Transformer transformer =
//                     stylesheetHandler.getTransformer();
//                     transformer.setOutputProperty(OutputKeys.ENCODING,
//                     "UTF-8");
//                     transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//                     transformer.setOutputProperty(
//                     "{http://xml.apache.org/xslt}indent-amount", "4");

                    stylesheetHandler.setResult(new SAXResult(state.driver
                            .getContentHandler()));
//                     stylesheetHandler.setResult(new
//                     StreamResult(System.out));
                    exportProject(state, stylesheetHandler);
                    resultFiles.add(state.outputFile);
                } catch (Exception e) {
                	cancel();
                	ExporterToPDF.this.getUIFacade().showErrorDialog(e);
                    return Status.CANCEL_STATUS;
                }
                finally {
                    monitor.worked(1);
                    if (out!=null) {
                        try {
                            out.flush();
                        	out.close();
                        } catch(IOException e) {
                            getUIFacade().showErrorDialog(e);
                        }
                    }
                }
                return Status.OK_STATUS;
            }
        };
        return result;
    }

    protected void exportProject(ExportState state, TransformerHandler handler)
            throws SAXException, ExportException {
        DateFormat df = java.text.DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
        handler.startDocument();
        AttributesImpl attrs = new AttributesImpl();
        addAttribute("xmlns:xsl", "http://www.w3.org/1999/XSL/Transform", attrs);
        addAttribute("xmlns:ganttproject", "http://ganttproject.sf.net/", attrs);
        addAttribute("version", "1.0", attrs);
        startElement("xsl:stylesheet", attrs, handler);
        // handler.startPrefixMapping("ganttproject",
        // "http://ganttproject.sf.net");

        writeViews(getUIFacade(), handler);

        addAttribute("xslfo-path", myStylesheet.getUrl().getPath(), attrs);
        startPrefixedElement("report", attrs, handler);
        addAttribute("xslfo-path", myStylesheet.getUrl().getPath(), attrs);
        addAttribute("title", i18n("ganttReport"), attrs);
        addAttribute("name", i18n("project"), attrs);
        addAttribute("nameValue", getProject().getProjectName(), attrs);
        addAttribute("organisation", i18n("organization"), attrs);
        addAttribute("organisationValue", getProject().getOrganization(), attrs);
        addAttribute("webLink", i18n("webLink"), attrs);
        addAttribute("webLinkValue", getProject().getWebLink(), attrs);
        addAttribute("currentDateTimeValue", df.format(new java.util.Date()),
                attrs);
        addAttribute("description", i18n("shortDescription"), attrs);

        addAttribute("begin", i18n("start"), attrs);
        addAttribute("beginValue", new GanttCalendar(getProject().getTaskManager().getProjectStart()).toString(), attrs);

        addAttribute("end", i18n("end"), attrs);
        addAttribute("endValue", new GanttCalendar(getProject().getTaskManager().getProjectEnd()).toString(), attrs);

        startPrefixedElement("project", attrs, handler);
        textElement("descriptionValue", attrs, getProject().getDescription(),
                handler);
        endPrefixedElement("project", handler);
        writeCharts(state, handler);
        writeTasks(getProject().getTaskManager(), handler);
        writeResources(getProject().getHumanResourceManager(), handler);
        endPrefixedElement("report", handler);
        // handler.endPrefixMapping("ganttproject");
        endElement("xsl:stylesheet", handler);
        handler.endDocument();
    }

    protected String getAssignedResourcesDelimiter() {
      return "\n\r";
    }

	private void writeCharts(ExportState state, TransformerHandler handler)
            throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        addAttribute("title", i18n("ganttChart"), attrs);
        addAttribute("src", state.ganttChartImageFile.getAbsolutePath(), attrs);
        startPrefixedElement("ganttchart", attrs, handler);
        endPrefixedElement("ganttchart", handler);

        addAttribute("title", i18n("resourcesChart"), attrs);
        addAttribute("src", state.resourceChartImageFile.getAbsolutePath(),
                attrs);
        startPrefixedElement("resourceschart", attrs, handler);
        endPrefixedElement("resourceschart", handler);
    }

    private Options createOptions() throws ExportException {
        JDKFontLocator locator = new JDKFontLocator();
        FontRecord[] fontRecords = locator.getFontRecords();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamResult output = new StreamResult(outputStream);
        try {
            TransformerHandler handler = getTransformerFactory()
                    .newTransformerHandler();
            handler.setResult(output);
            // just for nifty debugging :)
            // handler.getTransformer().setOutputProperty(OutputKeys.INDENT,
            // "yes");
            createConfiguration(handler, fontRecords);
        } catch (TransformerConfigurationException e) {
            throw new ExportException("Failed to create FOP options", e);
        } catch (SAXException e) {
            throw new ExportException("Failed to create FOP options", e);
        } catch (UnsupportedEncodingException e) {
            throw new ExportException("Failed to create FOP options", e);
		}
        Options result;
        try {
            result = new Options(new ByteArrayInputStream(outputStream
                    .toByteArray()));
        } catch (FOPException e) {
            throw new ExportException("Failed to create FOP options", e);
        }
        return result;
    }

    private void createConfiguration(TransformerHandler handler,
            FontRecord[] fontRecords) throws SAXException, UnsupportedEncodingException {
        AttributesImpl attrs = new AttributesImpl();
        handler.startDocument();
        handler.startElement("", "configuration", "configuration", attrs);
        handler.startElement("", "fonts", "fonts", attrs);

        for (int i = 0; i < fontRecords.length; i++) {
            FontRecord nextRecord = fontRecords[i];
            attrs.clear();
            String metricsFile = URLDecoder.decode(nextRecord.getMetricsLocation().toString(), "utf-8");
            attrs.addAttribute("", "metrics-file", "metrics-file", "CDATA", metricsFile);
            attrs.addAttribute("", "kerning", "kerning", "CDATA", "yes");
            attrs.addAttribute("", "embed-file", "embed-file", "CDATA",
                    nextRecord.getFontLocation().getPath());
            handler.startElement("", "font", "font", attrs);
            writeTriplets(handler, nextRecord.getFontTriplets());
            handler.endElement("", "font", "font");
        }
        handler.endElement("", "fonts", "fonts");
        handler.endElement("", "configuration", "configuration");
        handler.endDocument();
    }

    private void writeTriplets(TransformerHandler handler,
            FontTriplet[] fontTriplets) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        for (int i = 0; i < fontTriplets.length; i++) {
            FontTriplet next = fontTriplets[i];
            attrs.clear();
            attrs.addAttribute("", "name", "name", "CDATA", next.getName());
            attrs.addAttribute("", "style", "style", "CDATA", next.isItalic()
                    ? "italic"
                    : "normal");
            attrs.addAttribute("", "weight", "weight", "CDATA", next.isBold()
                    ? "bold"
                    : "normal");
            handler.startElement("", "font-triplet", "font-triplet", attrs);
            handler.endElement("", "font-triplet", "font-triplet");
        }
    }

    private static class ExportState {
        final File outputFile;
        public ExportState(File outputFile) {
            this.outputFile = outputFile;
        }
        Driver driver;
        File ganttChartImageFile;
        File resourceChartImageFile;
    }
    protected void setSelectedStylesheet(Stylesheet stylesheet) {
        myStylesheet = (PDFStylesheet) stylesheet;
    }

    protected String getStylesheetOptionID() {
        return "impex.pdf.stylesheet";
    }

    protected Stylesheet[] getStylesheets() {
        StylesheetFactoryImpl factory = new StylesheetFactoryImpl() {
            protected Stylesheet newStylesheet(URL resolvedUrl,
                    String localizedName) {
                return new PDFStylesheetImpl(resolvedUrl, localizedName);
            }
        };
        return (Stylesheet[]) factory.createStylesheets(PDFStylesheet.class);
    }

    private class PDFStylesheetImpl extends StylesheetImpl implements PDFStylesheet {
        PDFStylesheetImpl(URL stylesheetURL, String localizedName) {
            super(stylesheetURL, localizedName);
        }
    }
}
