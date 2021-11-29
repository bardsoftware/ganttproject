/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject Team

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

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.PrintChartApiImpl;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.export.ChartDimensions;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import net.sourceforge.ganttproject.chart.export.TreeTableApi;
import org.ganttproject.impex.htmlpdf.fonts.TTFontCache;

import java.awt.*;

/**
 * Provides functions for writing charts to PDF writer.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class ChartWriter implements ChartImageVisitor {
  private final Document myDoc;
  private final TimelineChart myChart;
  private final GanttExportSettings myExportSettings;
  private final PdfWriter myWriter;
  private final TTFontCache myFontCache;

  private float myScale;
  private float myYShift;
  private PdfTemplate myTemplate;
  private Graphics2D myGraphics;
  private final String myCharset;
  private final FontSubstitutionModel mySubstitutions;
  private int myOffsetY;

  ChartWriter(TimelineChart chart, PdfWriter writer, Document doc, GanttExportSettings exportSettings,
      TTFontCache fontCache, FontSubstitutionModel substitutionModel, String charset) {
    myChart = chart;
    myWriter = writer;
    myDoc = doc;
    myExportSettings = exportSettings;
    myFontCache = fontCache;
    myCharset = charset;
    mySubstitutions = substitutionModel;
  }

  protected ChartModel getModel() {
    return myChart.getModel();
  }

  void write() {
    setupChart(myExportSettings);
    var printChartApi = (PrintChartApiImpl)myChart.asPrintChartApi();
    printChartApi.buildImage(myExportSettings, this);
    myGraphics.dispose();
    myWriter.getDirectContent().addTemplate(myTemplate, myScale, 0, 0, myScale, myDoc.leftMargin(), myYShift);
  }

  private Graphics2D getGraphics(ChartDimensions d) {
    if (myGraphics == null) {
      myTemplate = myWriter.getDirectContent().createTemplate(d.getFullWidth(), d.getChartHeight());
      Rectangle page = myDoc.getPageSize();
      final float width = page.getWidth() - myDoc.leftMargin() - myDoc.rightMargin();
      final float height = page.getHeight() - myDoc.bottomMargin() - myDoc.topMargin();

      final float xscale = width / d.getFullWidth();
      final float yscale = height / d.getChartHeight();
      myScale = Math.min(xscale, yscale);
      myYShift = height - d.getChartHeight() * myScale + myDoc.bottomMargin();
      myGraphics = myTemplate.createGraphics(d.getFullWidth(), d.getChartHeight(),
          myFontCache.getFontMapper(mySubstitutions, myCharset));
      myGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }
    return myGraphics;
  }

  protected void setupChart(GanttExportSettings settings) {
    // myChart.gmyModel.setBounds(myModel.getMaxBounds());
  }

  @Override
  public void acceptLogo(ChartDimensions d, Image logo) {
    Graphics2D g = getGraphics(d);
    g.setBackground(Color.WHITE);
    g.clearRect(0, 0, d.getTreeWidth(), d.getLogoHeight());
    g.drawImage(logo, 0, 0, null);
  }

  @Override
  public void acceptTable(ChartDimensions d, TreeTableApi treeTableApi) {
    Graphics2D g = getGraphics(d);
    myOffsetY = d.getLogoHeight();
    g.translate(0, d.getLogoHeight());
    var header = treeTableApi.getTableHeaderComponent().invoke();
    if (header != null) {
      header.print(g);
      g.translate(0, d.getTableHeaderHeight());
      myOffsetY += d.getTableHeaderHeight();
    }
    var table = treeTableApi.getTableComponent().invoke();
    if (table != null) {
      table.print(g);
    } else {
      treeTableApi.getTablePainter().invoke(g);
    }
  }

  @Override
  public void acceptChart(ChartDimensions d, ChartModel model) {
    Graphics2D g = getGraphics(d);
    g.translate(d.getTreeWidth(), -myOffsetY);
    g.clip(new java.awt.Rectangle(d.getChartWidth(), d.getChartHeight()));
    model.paint(g);
  }
}
