/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011-2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.export;

import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPAbstractOption;
import biz.ganttproject.core.option.GPOption;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.impex.csv.GanttCSVExport;

public class ExporterToCSV extends ExporterBase {
  static class FileTypeOption extends GPAbstractOption<String> implements EnumerationOption {
    static final String[] FILE_FORMAT_ID = new String[] { "impex.csv.fileformat.csv", "impex.xls.fileformat.xls" };

    static final String[] FILE_EXTENSION = new String[] { "csv", "xls" };

    FileTypeOption() {
      super("impex.csv.fileformat");
    }

    @Override
    public String[] getAvailableValues() {
      return FileTypeOption.FILE_FORMAT_ID;
    }

    String proposeFileExtension() {
      for (int i = 0; i < FileTypeOption.FILE_FORMAT_ID.length; i++) {
        if (getValue().equals(FileTypeOption.FILE_FORMAT_ID[i])) {
          return FileTypeOption.FILE_EXTENSION[i];
        }
      }
      throw new IllegalStateException("Selected format=" + getValue() + " has not been found in known formats:"
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
  private final GPOptionGroup myOptions = new GPOptionGroup("impex.csv", new GPOption[] { myFileTypeOption });


  public ExporterToCSV() {
    myOptions.setTitled(false);
  }

  @Override
  public String getFileTypeDescription() {
    return GanttLanguage.getInstance().getText("impex.csv.description");
  }

  @Override
  public GPOptionGroup getOptions() {
    return myOptions;
  }

  @Override
  public List<GPOptionGroup> getSecondaryOptions() {
    return null;
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
    ExporterJob job = createCVSExportJob(outputFile);
    resultFiles.add(outputFile);
    return new ExporterJob[] { job };
  }

  private ExporterJob createCVSExportJob(final File outputFile) {
    ExporterJob result = new ExporterJob("Export project") {
      @Override
      protected IStatus run() {
        OutputStream outputStream = null;
        try {
          outputFile.createNewFile();
          outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
          // TODO Fix this ugly hack!! Ie make the settings available in a proper way
          GanttCSVExport exporter = new GanttCSVExport(getProject(),
              ((GanttProject) getProject()).getGanttOptions().getCSVOptions());
          exporter.save(outputStream, myFileTypeOption.proposeFileExtension());
        } catch (IOException e) {
          getUIFacade().showErrorDialog(e);
          return Status.CANCEL_STATUS;
        } finally {
          if (outputStream != null) {
            try {
              outputStream.close();
            } catch (IOException e) {
              GPLogger.logToLogger(e);
            }
          }
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
