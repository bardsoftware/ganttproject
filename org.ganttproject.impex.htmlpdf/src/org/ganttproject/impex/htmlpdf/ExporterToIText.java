/*
GanttProject is an opensource project management tool.
Copyright (C) 2009 Dmitry Barashev

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.export.ExportException;
import net.sourceforge.ganttproject.export.Exporter;
import net.sourceforge.ganttproject.export.TaskVisitor;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder.I18N;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.Task;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.ganttproject.impex.htmlpdf.fonts.TTFontCache;
import org.osgi.service.prefs.Preferences;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEvent;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

public class ExporterToIText extends ExporterBase implements Exporter{
    private ITextStylesheet myStylesheet;
    private TTFontCache myFontCache;
    private FontSubstitutionModel mySubstitutionModel;
    private Object myFontsMutex = new Object();
    private boolean myFontsReady = false;

    public ExporterToIText() {
        registerFonts();
    }

    public String getFileTypeDescription() {
        return GanttLanguage.getInstance().getText("impex.pdf.description")+" (iText beta)";
    }

    public GPOptionGroup[] getSecondaryOptions() {
        return ((ThemeImpl)myStylesheet).getOptions();
    }

    public String getFileNamePattern() {
        return "pdf";
    }

    public String proposeFileExtension() {
        return "pdf";
    }

    public Component getCustomOptionsUI() {
        waitRegisterFonts();
        JPanel result = new JPanel(new BorderLayout());
        result.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        OptionsPageBuilder builder = new OptionsPageBuilder();
        result.add(builder.buildPlanePage(getSecondaryOptions()), BorderLayout.NORTH);
        result.add(createFontPanel(), BorderLayout.CENTER);
        return result;
    }

    private Component createFontPanel() {
        return new FontSubstitutionPanel(mySubstitutionModel).getComponent();
    }

    public String[] getFileExtensions() {
        return new String[]{"pdf"};
    }

    protected void setSelectedStylesheet(Stylesheet stylesheet) {
        waitRegisterFonts();
        myStylesheet = (ITextStylesheet) stylesheet;
        if (getPreferences()!=null) {
            Preferences node = getPreferences().node("/configuration/org.ganttproject.impex.htmlpdf/font-substitution");
            mySubstitutionModel = new FontSubstitutionModel(myFontCache, myStylesheet, node);
            myStylesheet.setFontSubstitutionModel(mySubstitutionModel);
        }
    }

    public void setStylesheet(Stylesheet stylesheet) {
        myStylesheet = (ITextStylesheet) stylesheet;
    }

    protected String getStylesheetOptionID() {
        return "impex.pdf.stylesheet";
    }

    protected Stylesheet[] getStylesheets() {
        StylesheetFactoryImpl factory = new StylesheetFactoryImpl() {
            protected Stylesheet newStylesheet(URL resolvedUrl,
                    String localizedName) {
                return new ThemeImpl(resolvedUrl, localizedName);
            }
        };
        return (Stylesheet[]) factory.createStylesheets(ITextStylesheet.class);
    }

    private void registerFonts() {
        myFontCache = new TTFontCache();
        Thread fontReadingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                registerFontDirectories();
                synchronized (ExporterToIText.this.myFontsMutex) {
                    myFontsReady = true;
                    myFontsMutex.notifyAll();
                }
            }
        });
        fontReadingThread.setPriority(Thread.MIN_PRIORITY);
        fontReadingThread.start();
    }

    private void waitRegisterFonts() {
        while (myFontsMutex != null) {
            synchronized (myFontsMutex) {
                if (myFontsReady) {
                    break;
                }
                try {
                    myFontsMutex.wait();
                } catch (InterruptedException e) {
                    GPLogger.log(e);
                    break;
                }
            }
        }
    }

    protected void registerFontDirectories() {
        myFontCache.registerDirectory(System.getProperty("java.home") + "/lib/fonts", false);
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        IConfigurationElement[] configElements =
            extensionRegistry.getConfigurationElementsFor("org.ganttproject.impex.htmlpdf.FontDirectory");
        for (int i=0; i<configElements.length; i++) {
            final String dirName = configElements[i].getAttribute("name");
            if (Boolean.TRUE.toString().equalsIgnoreCase(configElements[i].getAttribute("absolute"))) {
                myFontCache.registerDirectory(dirName, true);
            } else {
                String namespace = configElements[i].getDeclaringExtension().getNamespaceIdentifier();
                URL dirUrl = Platform.getBundle(namespace).getResource(dirName);
                if (dirUrl==null) {
                    GPLogger.getLogger(getClass()).warning("Failed to find directory " + dirName + " in plugin " + namespace);
                    continue;
                }
                try {
                    URL resolvedDir = Platform.resolve(dirUrl);
                    myFontCache.registerDirectory(resolvedDir.getPath(), true);
                } catch (IOException e) {
                   GPLogger.log(e);
                   continue;
                }
            }
        }
    }

    protected Job[] createJobs(File outputFile, List<File> resultFiles) {
        waitRegisterFonts();
        return new Job[] {createTransformationJob(outputFile)};
    }

    private Job createTransformationJob(final File outputFile) {
        Job result = new ExportJob("Generating PDF") {
            protected IStatus run(IProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    getJobManager().cancel(ExporterBase.EXPORT_JOB_FAMILY);
                    return Status.CANCEL_STATUS;
                }
                assert myStylesheet!=null;
                OutputStream out = null;
                try {
                    out = new FileOutputStream(outputFile);
                    ((ThemeImpl)myStylesheet).run(getProject(), getUIFacade(), out);
                } catch (ExportException e) {
                    cancel();
                    e.printStackTrace();
                    getUIFacade().showErrorDialog(e);
                    return Status.CANCEL_STATUS;
                } catch (FileNotFoundException e) {
                    cancel();
                    e.printStackTrace();
                    getUIFacade().showErrorDialog(e);
                    return Status.CANCEL_STATUS;
                } finally {
                    monitor.worked(1);
                }
                return Status.OK_STATUS;
            }
        };
        return result;
    }

    static class ChartWriter {
        protected final ChartModel myModel;
        private PdfWriter myWriter;
        private Document myDoc;
        ChartWriter(TimelineChart chart, PdfWriter writer, Document doc) {
            myModel = chart.getModel();
            myWriter = writer;
            myDoc = doc;
        }

        void write() {
            setupChart();
            Dimension d = myModel.getBounds();
            d.height += myModel.getChartUIConfiguration().getHeaderHeight();

            PdfTemplate template = myWriter.getDirectContent().createTemplate(d.width, d.height);

            Rectangle page = myDoc.getPageSize();
            final float width = page.getWidth() - myDoc.leftMargin() - myDoc.rightMargin();
            final float height = page.getHeight() - myDoc.bottomMargin() - myDoc.topMargin();

            final float xscale = width/d.width;
            final float yscale = height/d.height;
            final float minscale = Math.min(xscale, yscale);
            Graphics2D g2 = template.createGraphics(d.width, d.height);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
            try {
                myModel.paint(g2);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            g2.dispose();
            float yshift = height - d.height * minscale + myDoc.bottomMargin();
            myWriter.getDirectContent().addTemplate(template, minscale, 0, 0, minscale, myDoc.leftMargin(), yshift);

        }

        protected void setupChart() {
            myModel.setBounds(myModel.getMaxBounds());
        }
    }

    static class ThemeImpl extends StylesheetImpl implements PdfPageEvent, ITextStylesheet {
        static List<String> ourSizes = new ArrayList<String>();
        static {
            Field[] fields = PageSize.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getType().equals(Rectangle.class)) {
                    ourSizes.add(fields[i].getName());
                }
            }
        }
        private static final Color SORTAVALA_GREEN = new Color(0x66, 0x99, 0x99);
        private Document myDoc;
        private PdfWriter myWriter;
        private IGanttProject myProject;
        private UIFacade myUIFacade;
        private String myLeftSubcolontitle;
        private BooleanOption myShowNotesOption = new DefaultBooleanOption("export.itext.showNotes");
        private BooleanOption myLandscapeOption = new DefaultBooleanOption("export.itext.landscape");
        private EnumerationOption myPageSizeOption = new DefaultEnumerationOption(
                "export.itext.pageSize", ourSizes);
        private GPOptionGroup myPageOptions = new GPOptionGroup("export.itext.page", new GPOption[] {
                myPageSizeOption, myLandscapeOption});
        private GPOptionGroup myDataOptions = new GPOptionGroup("export.itext.data", new GPOption[] {
                myShowNotesOption});
        private boolean isColontitleEnabled = false;
        private Properties myProperties;
        private FontSubstitutionModel mySubstitutionModel;

        ThemeImpl(URL url, String localizedName) {
            super(url, localizedName);
            myProperties = new Properties();
            try {
                myProperties.load(url.openStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            I18N i18n = new OptionsPageBuilder.I18N();
            myDataOptions.setI18Nkey(i18n.getCanonicalOptionGroupLabelKey(myDataOptions), "show");
            myDataOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myShowNotesOption), "notes");
            myDataOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myShowNotesOption)+".yes", "yes");
            myDataOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myShowNotesOption)+".no", "no");

            myPageOptions.setI18Nkey(i18n.getCanonicalOptionGroupLabelKey(myPageOptions), "choosePaperFormat");
            myPageOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myLandscapeOption)+".yes", "landscape");
            myPageOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myLandscapeOption)+".no", "portrait");
            myPageOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myPageSizeOption), "paperSize");

            myPageOptions.lock();
            myDataOptions.lock();
            myPageSizeOption.setValue("A4");
            myShowNotesOption.loadPersistentValue("true");
            myLandscapeOption.loadPersistentValue("true");
            myPageOptions.commit();
            myDataOptions.commit();
        }

        public GPOptionGroup[] getOptions() {
            return new GPOptionGroup[] {myDataOptions, myPageOptions};
        }

        protected IGanttProject getProject() {
            return myProject;
        }

        protected UIFacade getUIFacade() {
            return myUIFacade;
        }

        ///////////////////////////////////////
        // ITextStylesheet
        public List<String> getFontFamilies() {
            return Collections.singletonList(getOriginalFontName());
        }

        public void setFontSubstitutionModel(FontSubstitutionModel model) {
            mySubstitutionModel = model;
        }

        private String getOriginalFontName() {
            return myProperties.getProperty("font-family");
        }

        private String getFontName() {
            return mySubstitutionModel.getSubstitution(getOriginalFontName()).getSubstitutionFamily();
        }

        protected Font getSansRegular(float size) {
            return FontFactory.getFont(getFontName(), GanttLanguage.getInstance().getCharSet(), size);
        }

        protected Font getSansItalic(float size) {
            return FontFactory.getFont(getFontName(), GanttLanguage.getInstance().getCharSet(), size, Font.ITALIC);
        }

        protected Font getSansRegularBold(float size) {
            return FontFactory.getFont(getFontName(), GanttLanguage.getInstance().getCharSet(), size, Font.BOLD);
        }

        protected Font getSansRegularBold() {
            return getSansRegularBold(12);
        }

        void run(IGanttProject project, UIFacade facade, OutputStream out) throws ExportException {
            myProject = project;
            myUIFacade = facade;
            Rectangle pageSize = PageSize.getRectangle(myPageSizeOption.getValue());
            if (myLandscapeOption.isChecked()) {
                pageSize = pageSize.rotate();
            }
            myDoc = new Document(pageSize,  20, 20, 70, 40);
            try {
                myWriter = PdfWriter.getInstance(myDoc, out);
                myWriter.setPageEvent(this);
                myDoc.open();
                writeProject();
            } catch(Throwable e) {
                e.printStackTrace();
                throw new ExportException("Export failed", e);
            } finally {
                myDoc.close();
            }
        }

        public void writeProject() throws Exception {
            writeTitlePage();
            myDoc.newPage();
            isColontitleEnabled = true;
            myLeftSubcolontitle = "Tasks";
            writeTasks();
            myDoc.newPage();
            myLeftSubcolontitle = "Resources";
            writeResources();
            myDoc.newPage();
            writeGanttChart();
            myDoc.newPage();
            writeResourceChart();
        }

        private void writeAttributes(PdfPTable table, LinkedHashMap<String, String> attrs) {
            for (Iterator<Entry<String, String>> entries = attrs.entrySet().iterator(); entries.hasNext();) {
                Map.Entry<String, String> nextEntry = entries.next();
                {
                    Paragraph p = new Paragraph(nextEntry.getKey(), getSansRegularBold(12));
                    PdfPCell cell = new PdfPCell(p);
                    cell.setBorder(PdfPCell.NO_BORDER);
                    table.addCell(cell);
                }
                {
                    Paragraph p = new Paragraph(nextEntry.getValue(), getSansRegular(12));
                    PdfPCell cell = new PdfPCell(p);
                    cell.setBorder(PdfPCell.NO_BORDER);
                    table.addCell(cell);
                }
            }
        }

        private void writeTitlePage() throws DocumentException {
            Rectangle page = myDoc.getPageSize();
            PdfPTable head = new PdfPTable(1);
            PdfPTable colontitleTable = createColontitleTable(
                    getProject().getProjectName(),
                    GanttLanguage.getInstance().getMediumDateFormat().format(new Date()),
                    getProject().getOrganization(),
                    getProject().getWebLink());

            head.setTotalWidth(page.getWidth() - myDoc.leftMargin() - myDoc.rightMargin());
            {
                PdfPCell cell = new PdfPCell(colontitleTable);
                cell.setBorder(PdfPCell.NO_BORDER);
                head.addCell(cell);
            }
            addEmptyRow(head, 20);
            LinkedHashMap<String, String> attrs = new LinkedHashMap<String, String>();
            attrs.put("Project managers: ", buildManagerString());
            attrs.put("Dates: ", buildProjectDatesString());
            attrs.put(" ", " ");
            attrs.put("Complete:", buildProjectCompletionString());
            attrs.put("Tasks:", String.valueOf(getProject().getTaskManager().getTaskCount()));
            attrs.put("People:", String.valueOf(getProject().getHumanResourceManager().getResources().size()));
            PdfPTable attrsTable = new PdfPTable(2);
            writeAttributes(attrsTable, attrs);
            PdfPCell attrsCell = new PdfPCell(attrsTable);
            attrsCell.setBorder(PdfPCell.NO_BORDER);
            head.addCell(attrsCell);
            addEmptyRow(head, 20);
            if (getProject().getDescription().length() > 0) {
                Paragraph p = new Paragraph(getProject().getDescription(), getSansRegular(12));
                PdfPCell cell = new PdfPCell(p);
                cell.setBorder(PdfPCell.TOP | PdfPCell.BOTTOM);
                cell.setBorderColor(SORTAVALA_GREEN);
                cell.setBorderWidth(1);
                cell.setPadding(5);
                cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
                head.addCell(cell);
            }

            myDoc.add(head);
        }

        private String buildProjectCompletionString() {
            return String.valueOf(getProject().getTaskManager().getProjectCompletion()) + "%";
        }

        private String buildManagerString() {
            Role managerRole = getProject().getRoleManager().getRole(myProperties.getProperty("manager-role"));
            if (managerRole == null) {
                return "";
            }
            StringBuffer result = new StringBuffer();
            String delimiter = "";
            List<HumanResource> resources = getProject().getHumanResourceManager().getResources();
            for (int i = 0; i < resources.size(); i++) {
                HumanResource resource = resources.get(i);
                if (resource.getRole().equals(managerRole)) {
                    result.append(delimiter).append(resource.getName());
                    delimiter = ", ";
                }
            }
            return result.toString();
        }

        private String buildProjectDatesString() {
            DateFormat dateFormat = GanttLanguage.getInstance().getMediumDateFormat();
            return MessageFormat.format("{0} - {1}\n", new Object[] {
                    dateFormat.format(getProject().getTaskManager().getProjectStart()),
                    dateFormat.format(getProject().getTaskManager().getProjectEnd())});
        }

        private void writeGanttChart() {
            isColontitleEnabled = false;
            writeColontitle(
                    getProject().getProjectName(),
                    GanttLanguage.getInstance().getMediumDateFormat().format(new Date()),
                    GanttLanguage.getInstance().getText("ganttChart"),
                    String.valueOf(myWriter.getPageNumber()));
            ChartWriter ganttChartWriter = new ChartWriter(myUIFacade.getGanttChart(), myWriter, myDoc) {
                protected void setupChart() {
                    myModel.setVisibleTasks(Arrays.asList(getProject().getTaskManager().getTasks()));
                    super.setupChart();
                    //myModel.setRowHeight(myModel.getBounds().height/getProject().getTaskManager().getTaskCount());
                }
            };
            ganttChartWriter.write();
        }
        private void writeResourceChart() {
            isColontitleEnabled = false;
            writeColontitle(
                    getProject().getProjectName(),
                    GanttLanguage.getInstance().getMediumDateFormat().format(new Date()),
                    GanttLanguage.getInstance().getText("resourcesChart"),
                    String.valueOf(myWriter.getPageNumber()));
            ChartWriter resourceChartWriter = new ChartWriter((TimelineChart)myUIFacade.getResourceChart(), myWriter, myDoc) {
                protected void setupChart() {
                    super.setupChart();
                    //myModel.setRowHeight(myModel.getBounds().height/getProject().getH);
                }
            };
            resourceChartWriter.write();
        }


        protected PdfPTable createTableHeader(TableHeaderUIFacade tableHeader,
                ArrayList<Column> orderedColumns) throws DocumentException {
            for (int i = 0; i < tableHeader.getSize(); i++) {
                Column c = tableHeader.getField(i);
                if (c.isVisible()) {
                    orderedColumns.add(c);
                }
            }
            Collections.sort(orderedColumns, new Comparator<Column>() {
                public int compare(Column lhs, Column rhs) {
                    if (lhs == null || rhs == null) {
                        return 0;
                    }
                    return lhs.getOrder() - rhs.getOrder();
                }
            });
            float[] widths = new float[orderedColumns.size()];
            for (int i = 0; i < orderedColumns.size(); i++) {
                Column column = orderedColumns.get(i);
                widths[i] = (float) column.getWidth();
            }

            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(95);
            for (int i = 0; i < orderedColumns.size(); i++) {
                Column field = orderedColumns.get(i);
                if (field.isVisible()) {
                    PdfPCell cell = new PdfPCell(new Paragraph(field.getName(), getSansRegularBold(12f)));
                    cell.setPaddingTop(4);
                    cell.setPaddingBottom(4);
                    cell.setPaddingLeft(5);
                    cell.setPaddingRight(5);
                    cell.setBorderWidth(0);
                    cell.setBorder(PdfPCell.BOTTOM);
                    cell.setBorderWidthBottom(1);
                    cell.setBorderColor(new Color(0x66, 0x99, 0x99));
                    table.addCell(cell);
                }
            }
            table.setHeaderRows(1);
            return table;
        }

        protected void addEmptyRow(PdfPTable table, float height) {
            PdfPCell emptyCell = new PdfPCell(new Paragraph("  ", getSansRegular(height)));
            emptyCell.setBorderWidth(0);
            for (int i = 0; i < table.getNumberOfColumns(); i++) {
                table.addCell(emptyCell);
            }
        }

        protected void writeProperties(ArrayList<Column> orderedColumns,
                Map<String, String> id2value, PdfPTable table,
                Map<String, PdfPCell> id2cell) {
            for (int i=0; i<orderedColumns.size(); i++) {
                Column column = orderedColumns.get(i);
                PdfPCell cell = id2cell.get(column.getID());
                if (cell == null) {
                    String value = id2value.get(column.getID());
                    if (value == null) {
                        value = "";
                    }
                    Paragraph p = new Paragraph(value, getSansRegular(12));
                    cell = new PdfPCell(p);
                    cell.setBorderWidth(0);
                    cell.setPaddingLeft(5);
                }
                table.addCell(cell);
            }
        }

        private void writeTasks() throws Exception {
            TableHeaderUIFacade visibleFields = getUIFacade().getTaskTree().getVisibleFields();
            final ArrayList<Column> orderedColumns = new ArrayList<Column>();
            final PdfPTable table = createTableHeader(visibleFields, orderedColumns);

            TaskVisitor taskVisitor = new TaskVisitor() {
                int myPreviousChildTaskCount = 0;
                int myPreviousChildlessTaskCount = 0;

                PropertyFetcher myTaskProperty = new PropertyFetcher(getProject());
                protected String serializeTask(Task t, int depth) throws Exception {
                    boolean addEmptyRow = false;
                    if (depth == 0) {
                        addEmptyRow = myPreviousChildTaskCount > 0;
                        if (!addEmptyRow) {
                            boolean hasNested = getProject().getTaskManager().getTaskHierarchy().hasNestedTasks(t);
                            if (hasNested) {
                                addEmptyRow = myPreviousChildlessTaskCount > 0;
                                myPreviousChildlessTaskCount = 0;
                            } else {
                                myPreviousChildlessTaskCount++;
                            }
                        }
                        myPreviousChildTaskCount = 0;
                    }
                    else {
                        myPreviousChildTaskCount++;
                        myPreviousChildlessTaskCount = 0;
                    }
                    if (addEmptyRow) {
                        addEmptyRow(table, 10);
                    }
                    HashMap<String, String> id2value = new HashMap<String, String>();
                    myTaskProperty.getTaskAttributes(t, id2value);
                    HashMap<String, PdfPCell> id2cell = new HashMap<String, PdfPCell>();

                    PdfPCell nameCell;
                    if (myShowNotesOption.isChecked() && t.getNotes() != null
                            && !"".equals(t.getNotes())) {
                        nameCell = new PdfPCell(createNameCellContent(t));
                    } else {
                        nameCell = new PdfPCell(new Paragraph(t.getName(), getSansRegular(12)));
                    }
                    nameCell.setBorderWidth(0);
                    nameCell.setPaddingLeft(5 + depth * 10);

                    id2cell.put("tpd3", nameCell);
                    writeProperties(orderedColumns, id2value, table, id2cell);
                    return "";
                }

                private PdfPTable createNameCellContent(Task t) throws BadElementException {
                    PdfPTable table = new PdfPTable(1);
                    Paragraph p = new Paragraph(t.getName(), getSansRegular(12));
                    PdfPCell cell1 = new PdfPCell();
                    cell1.setBorder(PdfPCell.NO_BORDER);
                    cell1.setPhrase(p);
                    cell1.setPaddingLeft(0);
                    table.addCell(cell1);

                    Paragraph notes = new Paragraph(t.getNotes(), getSansItalic(8));
                    PdfPCell cell2 = new PdfPCell();
                    cell2.setBorder(PdfPCell.NO_BORDER);
                    cell2.setPhrase(notes);
                    cell2.setPaddingLeft(3);
                    table.addCell(cell2);
                    return table;
                }
            };
            taskVisitor.visit(getProject().getTaskManager());
            myDoc.add(table);
        }


        private void writeResources() throws Exception {
            TableHeaderUIFacade visibleFields = getUIFacade().getResourceTree().getVisibleFields();
            final ArrayList<Column> orderedColumns = new ArrayList<Column>();
            final PdfPTable table = createTableHeader(visibleFields, orderedColumns);
            List<HumanResource> resources = getProject().getHumanResourceManager().getResources();

            PropertyFetcher propFetcher = new PropertyFetcher(getProject());
            for (int i = 0; i < resources.size(); i++) {
                HumanResource resource = resources.get(i);
                HashMap<String, String> id2value = new HashMap<String, String>();
                propFetcher.getResourceAttributes(resource, id2value);
                HashMap<String, PdfPCell> id2cell = new HashMap<String, PdfPCell>();
                writeProperties(orderedColumns, id2value, table, id2cell);
            }
            myDoc.add(table);

        }

        PdfPTable createColontitleTable(String topLeft, String topRight, String bottomLeft, String bottomRight) {
            PdfPTable head = new PdfPTable(2);
            {
                PdfPCell cell = new PdfPCell();
                cell.setBorder(Rectangle.NO_BORDER);
                Paragraph p = new Paragraph(topLeft, getSansRegularBold(18));
                p.setAlignment(Paragraph.ALIGN_LEFT);
                //colontitle.setLeading(0);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
                //cell.setPaddingLeft(2);
                cell.setPaddingBottom(6);
                cell.setPhrase(p);
                head.addCell(cell);
            }
            {
                PdfPCell cell = new PdfPCell();
                cell.setBorder(Rectangle.NO_BORDER);
                Paragraph p = new Paragraph(
                        topRight,
                        getSansRegularBold(10));
                p.setAlignment(Paragraph.ALIGN_RIGHT);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
                cell.setPaddingBottom(6);
                cell.setPhrase(p);
                head.addCell(cell);
            }
            {
                PdfPCell cell = new PdfPCell();
                cell.setVerticalAlignment(Element.ALIGN_TOP);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                cell.setPaddingLeft(3);
                cell.setPaddingTop(2);
                cell.setPaddingBottom(6);
                cell.setBorder(Rectangle.TOP);
                cell.setBorderWidthTop(2);
                cell.setBorderColor(SORTAVALA_GREEN);
                Paragraph p = new Paragraph(bottomLeft, getSansRegularBold(18));
                p.setAlignment(Paragraph.ALIGN_LEFT);
                p.setExtraParagraphSpace(0);
                p.setIndentationLeft(0);
                p.setSpacingBefore(0);
                cell.setPhrase(p);
                //cell.addElement(p);
                head.addCell(cell);
            }
            {
                PdfPCell cell = new PdfPCell();
                cell.setVerticalAlignment(Element.ALIGN_TOP);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell.setPaddingTop(2);
                cell.setPaddingBottom(6);
                cell.setBorder(Rectangle.TOP);
                cell.setBorderWidthTop(2);
                cell.setBorderColor(SORTAVALA_GREEN);
                Paragraph p = new Paragraph(bottomRight,getSansRegularBold(10));
                p.setAlignment(Paragraph.ALIGN_RIGHT);
                cell.setPhrase(p);
                head.addCell(cell);
            }
            final Document document = myDoc;
            Rectangle page = document.getPageSize();
            head.setTotalWidth(page.getWidth() - document.leftMargin() - document.rightMargin());
            return head;
        }

        private void writeColontitle(String topLeft, String topRight, String bottomLeft, String bottomRight) {
            final Document document = myDoc;
            final PdfWriter writer = myWriter;
            Rectangle page = document.getPageSize();
            PdfPTable colontitleTable = createColontitleTable(topLeft, topRight, bottomLeft, bottomRight);
            colontitleTable.writeSelectedRows(0, -1,
                    document.leftMargin(),
                    page.getHeight() - document.topMargin() + colontitleTable.getTotalHeight(),
                writer.getDirectContent());

        }

        public void onChapter(PdfWriter arg0, Document arg1, float arg2,
                Paragraph arg3) {
        }

        public void onChapterEnd(PdfWriter arg0, Document arg1, float arg2) {
        }

        public void onCloseDocument(PdfWriter arg0, Document arg1) {
        }

        public void onEndPage(PdfWriter writer, Document document) {
            if (isColontitleEnabled) {
                writeColontitle(
                        getProject().getProjectName(),
                        GanttLanguage.getInstance().getMediumDateFormat().format(new Date()),
                        myLeftSubcolontitle,
                        String.valueOf(writer.getPageNumber()));
            }
        }

        public void onGenericTag(PdfWriter arg0, Document arg1, Rectangle arg2, String arg3) {
        }

        public void onOpenDocument(PdfWriter arg0, Document arg1) {
        }

        public void onParagraph(PdfWriter arg0, Document arg1, float arg2) {
        }

        public void onParagraphEnd(PdfWriter arg0, Document arg1, float arg2) {
        }

        public void onSection(PdfWriter arg0, Document arg1, float arg2, int arg3, Paragraph arg4) {
        }

        public void onSectionEnd(PdfWriter arg0, Document arg1, float arg2) {
        }

        public void onStartPage(PdfWriter writer, Document document) {
        }
    }
}
