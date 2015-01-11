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

import java.awt.Component;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPAbstractOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
public class ExporterToImage extends ExporterBase {

  static class FileTypeOption extends GPAbstractOption<String> implements EnumerationOption {
    static final String[] FILE_FORMAT_ID = new String[] { "impex.image.fileformat.png", "impex.image.fileformat.jpeg" };

    static final String[] FILE_EXTENSION = new String[] { "png", "jpg" };

    // TODO GPAbstractOption already has this field, why add it again?!
    private String myValue = FileTypeOption.FILE_FORMAT_ID[0];

    FileTypeOption() {
      super("impex.image.fileformat");
    }

    @Override
    public String[] getAvailableValues() {
      return FileTypeOption.FILE_FORMAT_ID;
    }

    @Override
    public void setValue(String value) {
      myValue = value;
    }

    @Override
    public String getValue() {
      return myValue;
    }

    String proposeFileExtension() {
      for (int i = 0; i < FileTypeOption.FILE_FORMAT_ID.length; i++) {
        if (myValue.equals(FileTypeOption.FILE_FORMAT_ID[i])) {
          return FileTypeOption.FILE_EXTENSION[i];
        }
      }
      throw new IllegalStateException("Selected format=" + myValue + " has not been found in known formats:"
          + Arrays.asList(FileTypeOption.FILE_FORMAT_ID));
    }

    @Override
    public String getPersistentValue() {
      return null;
    }

    @Override
    public void loadPersistentValue(String value) {
    }

    @Override
    public boolean isChanged() {
      return false;
    }
  }

  private final FileTypeOption myFileTypeOption = new FileTypeOption();

  private final GPOptionGroup myOptions = new GPOptionGroup("impex.image", new GPOption[] { myFileTypeOption });

  public ExporterToImage() {
    myOptions.setTitled(false);
  }

  @Override
  public String getFileTypeDescription() {
    return MessageFormat.format(GanttLanguage.getInstance().getText("impex.image.description"),
        new Object[] { proposeFileExtension() });
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
    ExporterJob result = new ExporterJob("Export project") {
      @Override
      protected IStatus run() {
        Chart chart = getUIFacade().getActiveChart();

        // Test if there is an active chart
        if (chart == null) {
          // If not, it means we are running CLI
          String chartToExport = getPreferences().get("chart", null);

          // Default is to print Gantt chart
          chart = "resource".equals(chartToExport) ? getResourceChart() : getGanttChart();
        }
        RenderedImage renderedImage = chart.getRenderedImage(createExportSettings());
        try {
          ImageIO.write(renderedImage, myFileTypeOption.proposeFileExtension(), outputFile);
        } catch (IOException e) {
          getUIFacade().showErrorDialog(e);
          return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
      }
    };
    return result;
  }

  @Override
  public String proposeFileExtension() {
    return myFileTypeOption.proposeFileExtension();
  }

  @Override
  public String[] getFileExtensions() {
    return FileTypeOption.FILE_EXTENSION;
  }
}
