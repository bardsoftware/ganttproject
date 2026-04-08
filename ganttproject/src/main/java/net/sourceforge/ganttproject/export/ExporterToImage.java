/*
Copyright 2005-2012 GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.export;

import biz.ganttproject.core.option.*;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.eclipse.core.runtime.Status;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author bard
 */
public class ExporterToImage extends ExporterBase {

  private final ObservableEnumerationOption<ImageFileFormat> myFileTypeOption =
    new ObservableEnumerationOption<>("impex.image.fileformat", ImageFileFormat.PNG, Arrays.stream(ImageFileFormat.values()).toList());
//  private final ExportFileFormatOption<ImageFileFormat> myFileTypeOption =
//    new ExportFileFormatOption<>("impex.image.fileformat", ImageFileFormat.PNG, Arrays.stream(ImageFileFormat.values()).toList());

  private final GPOptionGroup myOptions = new GPOptionGroup("impex.image", myFileTypeOption);

  public ExporterToImage() {
    myOptions.setTitled(false);
  }

  private String getSelectedFormatExtension() {
    return myFileTypeOption.getSelectedValue().getExtension();
  }
  @Override
  protected void setFormat(String format) {
    ImageFileFormat.getEntries().stream().filter( f -> f.getExtension().equalsIgnoreCase(format))
      .findFirst().ifPresent(myFileTypeOption::setSelectedValue);
  }

  @Override
  public String getFileTypeDescription() {
    return MessageFormat.format(GanttLanguage.getInstance().getText("impex.image.description"),
      proposeFileExtension());
  }

  @Override
  public GPOptionGroup getOptions() {
    return myOptions;
  }

  @Override
  public List<GPOptionGroup> getSecondaryOptions() {
    return Collections.singletonList(createExportRangeOptionGroup());
  }

  @Override
  public Component getCustomOptionsUI() {
    return null;
  }

  @Override
  public String getFileNamePattern() {
    return proposeFileExtension();
  }

  @Override
  protected ExporterJob[] createJobs(final File outputFile, List<File> resultFiles) {
    ExporterJob job = createImageExportJob(outputFile);
    resultFiles.add(outputFile);
    return new ExporterJob[] { job };
  }

  private ExporterJob createImageExportJob(final File outputFile) {
    return new ImageExportJob("Export project", () -> {
      Chart chart = getUIFacade().getActiveChart();

      // Test if there is an active chart
      if (chart == null) {
        // If not, it means we are running CLI
        String chartToExport = getPreferences().get("chart", null);

        // Default is to print Gantt chart
        chart = "resource".equals(chartToExport) ? getResourceChart() : getGanttChart();
      }
      var exportSettings = createExportSettings();
      int zoomLevel = getPreferences().getInt("zoom", -1);
      RenderedImage renderedImage = chart.asPrintChartApi().exportChart(
          exportSettings.getStartDate(), exportSettings.getEndDate(), zoomLevel, exportSettings.isCommandLineMode());
      try {
        ImageIO.write(renderedImage, getSelectedFormatExtension(), outputFile);
      } catch (IOException e) {
        getUIFacade().showErrorDialog(e);
        return Status.CANCEL_STATUS;
      }
      return Status.OK_STATUS;
    });
  }

  @Override
  public String proposeFileExtension() {
    return getSelectedFormatExtension();
  }

  @Override
  public String[] getFileExtensions() {
    return ImageFileFormat.getEntries().stream().map(ImageFileFormat::getExtension).toArray(String[]::new);
  }
}
