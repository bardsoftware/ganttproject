/*
GanttProject is an opensource project management tool. License: GPL3
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
package org.ganttproject.impex.htmlpdf.itext;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.table.ColumnList.Column;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEvent;
import com.itextpdf.text.pdf.PdfWriter;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ResourceDefaultColumn;
import net.sourceforge.ganttproject.export.ExportException;
import net.sourceforge.ganttproject.export.ExporterBase;
import net.sourceforge.ganttproject.export.TaskVisitor;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder.I18N;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.LanguageOption;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.Task;
import org.ganttproject.impex.htmlpdf.PropertyFetcher;
import org.ganttproject.impex.htmlpdf.StylesheetImpl;
import org.ganttproject.impex.htmlpdf.fonts.TTFontCache;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Implements Sortavala iText theme.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class ThemeImpl extends StylesheetImpl implements PdfPageEvent, ITextStylesheet {
  private static List<String> ourSizes = new ArrayList<>();
  static {
    for (Field field : PageSize.class.getDeclaredFields()) {
      if (field.getType().equals(Rectangle.class)) {
        ourSizes.add(field.getName());
      }
    }
  }
  private static final BaseColor SORTAVALA_GREEN = new BaseColor(0x66, 0x99, 0x99);
  private Document myDoc;
  private PdfWriter myWriter;
  private IGanttProject myProject;
  private UIFacade myUIFacade;
  private String myLeftSubcolontitle;
  private final BooleanOption myShowNotesOption = new DefaultBooleanOption("export.itext.showNotes");
  private final BooleanOption myLandscapeOption = new DefaultBooleanOption("export.itext.landscape");
  private final EnumerationOption myPageSizeOption = new DefaultEnumerationOption<String>("export.itext.pageSize",
      ourSizes);
  private final LanguageOption myLanguageOption = new LanguageOption() {
    {
      setSelectedValue(GanttLanguage.getInstance().getLocale());
    }

    @Override
    protected void applyLocale(Locale locale) {
    }
  };
  private final GPOptionGroup myPageOptions = new GPOptionGroup("export.itext.page", myPageSizeOption,
      myLandscapeOption);
  private final GPOptionGroup myDataOptions = new GPOptionGroup("export.itext.data",
      myShowNotesOption);
  private boolean isColontitleEnabled = false;
  private final Properties myProperties;
  private FontSubstitutionModel mySubstitutionModel;
  private final ExporterBase myExporter;
  private final TTFontCache myFontCache;

  ThemeImpl(URL url, String localizedName, ExporterBase exporter, TTFontCache fontCache) {
    super(url, localizedName + " (iText)");
    myFontCache = fontCache;
    myExporter = exporter;
    myProperties = new Properties();
    try {
      myProperties.load(url.openStream());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    myFontCache.setProperties(myProperties);
    I18N i18n = new OptionsPageBuilder.I18N();
    GPOptionGroup languageOptions = new GPOptionGroup("export.itext.language", myLanguageOption);
    languageOptions.setI18Nkey(i18n.getCanonicalOptionGroupLabelKey(languageOptions), "language");
    languageOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myLanguageOption), "language");

    myDataOptions.setI18Nkey(i18n.getCanonicalOptionGroupLabelKey(myDataOptions), "show");
    myDataOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myShowNotesOption), "notes");
    myDataOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myShowNotesOption) + ".yes", "yes");
    myDataOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myShowNotesOption) + ".no", "no");

    myPageOptions.setI18Nkey(i18n.getCanonicalOptionGroupLabelKey(myPageOptions), "choosePaperFormat");
    myPageOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myLandscapeOption) + ".yes", "landscape");
    myPageOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myLandscapeOption) + ".no", "portrait");
    myPageOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myPageSizeOption), "paperSize");

    myPageOptions.lock();
    myDataOptions.lock();
    myPageSizeOption.setValue("A4");
    myShowNotesOption.loadPersistentValue("true");
    myLandscapeOption.loadPersistentValue("true");
    myPageOptions.commit();
    myDataOptions.commit();
  }

  GPOptionGroup[] getOptions() {
    return new GPOptionGroup[] { myDataOptions, myPageOptions };
  }

  protected IGanttProject getProject() {
    return myProject;
  }

  private UIFacade getUIFacade() {
    return myUIFacade;
  }

  // /////////////////////////////////////
  // ITextStylesheet
  @Override
  public List<String> getFontFamilies() {
    return Collections.singletonList(getOriginalFontName());
  }

  @Override
  public void setFontSubstitutionModel(FontSubstitutionModel model) {
    mySubstitutionModel = model;
  }

  private String getOriginalFontName() {
    return myProperties.getProperty("font-family");
  }

  private String getFontName() {
    return mySubstitutionModel.getSubstitution(getOriginalFontName()).getSubstitutionFamily();
  }

  private Font getSansRegular(float size) {
    return myFontCache.getFont(getFontName(), getCharset(), Font.NORMAL, size);
  }

  private Font getSansItalic(float size) {
    return myFontCache.getFont(getFontName(), getCharset(), Font.ITALIC, size);
  }

  private Font getSansRegularBold(float size) {
    return myFontCache.getFont(getFontName(), getCharset(), Font.BOLD, size);
  }

  protected Font getSansRegularBold() {
    return getSansRegularBold(12);
  }

  private String getCharset() {
    return myProperties.getProperty("charset", GanttLanguage.getInstance().getCharSet());
  }

  private String i18n(String key) {
    String value = myProperties.getProperty(key);
    if (value != null) {
      return value;
    }
//    Locale selectedLocale = myLanguageOption.getSelectedValue();
//    if (selectedLocale != null) {
//      value = GanttLanguage.getInstance().getText("impex.pdf.theme.sortavala." + key, selectedLocale);
//    }
//    if (value != null) {
//      return value;
//    }
    value = GanttLanguage.getInstance().getText("impex.pdf.theme.sortavala." + key);
    return value == null ? key : value;
  }

  void run(IGanttProject project, UIFacade facade, OutputStream out) throws ExportException {
    myProject = project;
    myUIFacade = facade;
    Rectangle pageSize = PageSize.getRectangle(myPageSizeOption.getValue());
    if (myLandscapeOption.isChecked()) {
      pageSize = pageSize.rotate();
    }
    myDoc = new Document(pageSize, 20, 20, 70, 40);
    try {
      myWriter = PdfWriter.getInstance(myDoc, out);
      myWriter.setPageEvent(this);
      myDoc.open();
      writeProject();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new ExportException("Export failed", e);
    } finally {
      myDoc.close();
    }
  }

  private void writeProject() throws Exception {
    writeTitlePage();
    myDoc.newPage();
    isColontitleEnabled = true;
    myLeftSubcolontitle = i18n("title.tasks");
    writeTasks();
    myDoc.newPage();
    myLeftSubcolontitle = i18n("title.resources");
    writeResources();
    myDoc.newPage();
    writeGanttChart();
    myDoc.newPage();
    writeResourceChart();
  }

  private void writeAttributes(PdfPTable table, LinkedHashMap<String, String> attrs) {
    for (Entry<String, String> nextEntry : attrs.entrySet()) {
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
    PdfPTable colontitleTable = createColontitleTable(getProject().getProjectName(),
        GanttLanguage.getInstance().getMediumDateFormat().format(new Date()), getProject().getOrganization(),
        getProject().getWebLink());

    head.setTotalWidth(page.getWidth() - myDoc.leftMargin() - myDoc.rightMargin());
    {
      PdfPCell cell = new PdfPCell(colontitleTable);
      cell.setBorder(PdfPCell.NO_BORDER);
      head.addCell(cell);
    }
    addEmptyRow(head, 20);
    LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
    attrs.put(i18n("label.project_manager"), buildManagerString());
    attrs.put(i18n("label.dates"), buildProjectDatesString());
    attrs.put(" ", " ");
    attrs.put(i18n("label.completion"), buildProjectCompletionString());
    attrs.put(i18n("label.tasks"), String.valueOf(getProject().getTaskManager().getTaskCount()));
    attrs.put(i18n("label.resources"), String.valueOf(getProject().getHumanResourceManager().getResources().size()));
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
    StringBuilder result = new StringBuilder();
    String delimiter = "";
    List<HumanResource> resources = getProject().getHumanResourceManager().getResources();
    for (HumanResource resource : resources) {
      if (resource.getRole().equals(managerRole)) {
        result.append(delimiter).append(resource.getName());
        delimiter = ", ";
      }
    }
    return result.toString();
  }

  private String buildProjectDatesString() {
    DateFormat dateFormat = GanttLanguage.getInstance().getMediumDateFormat();
    return MessageFormat.format(
        "{0} - {1}\n",
        dateFormat.format(getProject().getTaskManager().getProjectStart()),
        dateFormat.format(getProject().getTaskManager().getProjectEnd()));
  }

  private void writeGanttChart() {
    isColontitleEnabled = false;
    writeColontitle(getProject().getProjectName(),
        GanttLanguage.getInstance().getMediumDateFormat().format(new Date()),
        GanttLanguage.getInstance().getText("ganttChart"), String.valueOf(myWriter.getPageNumber()));
    ChartWriter ganttChartWriter = new ChartWriter(myUIFacade.getGanttChart(), myWriter, myDoc,
        myExporter.createExportSettings(), myFontCache, mySubstitutionModel, getCharset());
    ganttChartWriter.write();
  }

  private void writeResourceChart() {
    isColontitleEnabled = false;
    writeColontitle(getProject().getProjectName(),
        GanttLanguage.getInstance().getMediumDateFormat().format(new Date()),
        GanttLanguage.getInstance().getText("resourcesChart"), String.valueOf(myWriter.getPageNumber()));
    ChartWriter resourceChartWriter = new ChartWriter(myUIFacade.getResourceChart(), myWriter, myDoc,
        myExporter.createExportSettings(), myFontCache, mySubstitutionModel, getCharset());
    resourceChartWriter.write();
  }

  private PdfPTable createTableHeader(ColumnList tableHeader, ArrayList<Column> orderedColumns) {
    for (int i = 0; i < tableHeader.getSize(); i++) {
      Column c = tableHeader.getField(i);
      if (c.isVisible()) {
        orderedColumns.add(c);
      }
    }
    Collections.sort(orderedColumns, new Comparator<Column>() {
      @Override
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
      widths[i] = column.getWidth();
    }

    PdfPTable table = new PdfPTable(widths);
    table.setWidthPercentage(95);
    for (Column field : orderedColumns) {
      if (field.isVisible()) {
        PdfPCell cell = new PdfPCell(new Paragraph(field.getName(), getSansRegularBold(12f)));
        cell.setPaddingTop(4);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(5);
        cell.setPaddingRight(5);
        cell.setBorderWidth(0);
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setBorderWidthBottom(1);
        cell.setBorderColor(SORTAVALA_GREEN);
        table.addCell(cell);
      }
    }
    table.setHeaderRows(1);
    return table;
  }

  private void addEmptyRow(PdfPTable table, float height) {
    PdfPCell emptyCell = new PdfPCell(new Paragraph("  ", getSansRegular(height)));
    emptyCell.setBorderWidth(0);
    for (int i = 0; i < table.getNumberOfColumns(); i++) {
      table.addCell(emptyCell);
    }
  }

  private void writeProperties(ArrayList<Column> orderedColumns, Map<String, String> id2value, PdfPTable table,
                               Map<String, PdfPCell> id2cell) {
    for (Column column : orderedColumns) {
      PdfPCell cell = id2cell.get(column.getID());
      if (cell == null) {
        String value = id2value.get(column.getID());
        if (value == null) {
          value = "";
        }
        Paragraph p = new Paragraph(value, getSansRegular(12));
        cell = new PdfPCell(p);
        if (TaskDefaultColumn.COST.getStub().getID().equals(column.getID())
            || ResourceDefaultColumn.STANDARD_RATE.getStub().getID().equals(column.getID())) {
          cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
        }
        cell.setBorderWidth(0);
        cell.setPaddingLeft(5);
      }
      table.addCell(cell);
    }
  }

  private void writeTasks() throws Exception {
    ColumnList visibleFields = getUIFacade().getTaskTree().getVisibleFields();
    final ArrayList<Column> orderedColumns = new ArrayList<>();
    final PdfPTable table = createTableHeader(visibleFields, orderedColumns);

    TaskVisitor taskVisitor = new TaskVisitor() {
      int myPreviousChildTaskCount = 0;
      int myPreviousChildlessTaskCount = 0;

      PropertyFetcher myTaskProperty = new PropertyFetcher(getProject());

      @Override
      protected String serializeTask(Task t, int depth) throws Exception {
        boolean addEmptyRow = false;
        if (depth == 0) {
          addEmptyRow = myPreviousChildTaskCount > 0;
          boolean hasNested = getProject().getTaskManager().getTaskHierarchy().hasNestedTasks(t);
          if (!addEmptyRow) {
            if (hasNested) {
              addEmptyRow = myPreviousChildlessTaskCount > 0;
              myPreviousChildlessTaskCount = 0;
            }
          }
          myPreviousChildTaskCount = 0;
          if (!hasNested) {
            myPreviousChildlessTaskCount++;
          }
        } else {
          myPreviousChildTaskCount++;
          myPreviousChildlessTaskCount = 0;
        }
        if (addEmptyRow) {
          addEmptyRow(table, 10);
        }
        HashMap<String, String> id2value = new HashMap<>();
        myTaskProperty.getTaskAttributes(t, id2value);
        HashMap<String, PdfPCell> id2cell = new HashMap<>();

        PdfPCell nameCell;
        if (myShowNotesOption.isChecked() && t.getNotes() != null && !"".equals(t.getNotes())) {
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

      private PdfPTable createNameCellContent(Task t) {
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
    ColumnList visibleFields = getUIFacade().getResourceTree().getVisibleFields();
    final ArrayList<Column> orderedColumns = new ArrayList<>();
    final PdfPTable table = createTableHeader(visibleFields, orderedColumns);
    List<HumanResource> resources = getProject().getHumanResourceManager().getResources();

    PropertyFetcher propFetcher = new PropertyFetcher(getProject());
    for (HumanResource resource : resources) {
      HashMap<String, String> id2value = new HashMap<>();
      propFetcher.getResourceAttributes(resource, id2value);
      HashMap<String, PdfPCell> id2cell = new HashMap<>();
      writeProperties(orderedColumns, id2value, table, id2cell);
    }
    myDoc.add(table);

  }

  private PdfPTable createColontitleTable(String topLeft, String topRight, String bottomLeft, String bottomRight) {
    PdfPTable head = new PdfPTable(2);
    {
      PdfPCell cell = new PdfPCell();
      cell.setBorder(Rectangle.NO_BORDER);
      Paragraph p = new Paragraph(topLeft, getSansRegularBold(18));
      p.setAlignment(Paragraph.ALIGN_LEFT);
      // colontitle.setLeading(0);
      cell.setHorizontalAlignment(Element.ALIGN_LEFT);
      cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
      // cell.setPaddingLeft(2);
      cell.setPaddingBottom(6);
      cell.setPhrase(p);
      head.addCell(cell);
    }
    {
      PdfPCell cell = new PdfPCell();
      cell.setBorder(Rectangle.NO_BORDER);
      Paragraph p = new Paragraph(topRight, getSansRegularBold(10));
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
      // cell.addElement(p);
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
      Paragraph p = new Paragraph(bottomRight, getSansRegularBold(10));
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
    colontitleTable.writeSelectedRows(0, -1, document.leftMargin(), page.getHeight() - document.topMargin()
        + colontitleTable.getTotalHeight(), writer.getDirectContent());

  }

  @Override
  public void onChapter(PdfWriter arg0, Document arg1, float arg2, Paragraph arg3) {
  }

  @Override
  public void onChapterEnd(PdfWriter arg0, Document arg1, float arg2) {
  }

  @Override
  public void onCloseDocument(PdfWriter arg0, Document arg1) {
  }

  @Override
  public void onEndPage(PdfWriter writer, Document document) {
    if (isColontitleEnabled) {
      writeColontitle(getProject().getProjectName(),
          GanttLanguage.getInstance().getMediumDateFormat().format(new Date()), myLeftSubcolontitle,
          String.valueOf(writer.getPageNumber()));
    }
  }

  @Override
  public void onGenericTag(PdfWriter arg0, Document arg1, Rectangle arg2, String arg3) {
  }

  @Override
  public void onOpenDocument(PdfWriter arg0, Document arg1) {
  }

  @Override
  public void onParagraph(PdfWriter arg0, Document arg1, float arg2) {
  }

  @Override
  public void onParagraphEnd(PdfWriter arg0, Document arg1, float arg2) {
  }

  @Override
  public void onSection(PdfWriter arg0, Document arg1, float arg2, int arg3, Paragraph arg4) {
  }

  @Override
  public void onSectionEnd(PdfWriter arg0, Document arg1, float arg2) {
  }

  @Override
  public void onStartPage(PdfWriter writer, Document document) {
  }
}
