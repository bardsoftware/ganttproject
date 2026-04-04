/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010-2012 Dmitry Barashev, GanttProject Team

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
package biz.ganttproject.impex.msproject2;

import biz.ganttproject.core.option.*;
import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.mpx.MPXWriter;
import net.sf.mpxj.mspdi.MSPDIWriter;
import net.sf.mpxj.writer.ProjectWriter;
import net.sourceforge.ganttproject.export.ExporterBase;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class ExporterToMsProjectFile extends ExporterBase {

  private final ObservableEnumerationOption<MsProjectFileFormat> myFileFormatOption = new ObservableEnumerationOption<>(
    "impex.msproject.fileformat", MsProjectFileFormat.MPX, Arrays.stream(MsProjectFileFormat.values()).toList()
  );
  //private final LocaleOption myLanguageOption = new LocaleOption();
  private final ExportMPXLocaleOption myLanguageOption = new ExportMPXLocaleOption();

  private final GPOptionGroup myOptions = new GPOptionGroup("exporter.msproject", new GPOption[] { myFileFormatOption });

  private final GPOptionGroup myMPXOptions = new GPOptionGroup("exporter.msproject.mpx", new GPOption[] { myLanguageOption });

  public ExporterToMsProjectFile() {
    myOptions.setTitled(false);
    myMPXOptions.setTitled(false);
    //myLanguageOption.setSelectedLocale(language.getLocale());
  }

  @Override
  public String getFileTypeDescription() {
    return language.getText("impex.msproject.description");
  }

  @Override
  public GPOptionGroup getOptions() {
    return myOptions;
  }

  @Override
  public List<GPOptionGroup> getSecondaryOptions() {
    return myFileFormatOption.getSelectedValue() == MsProjectFileFormat.MPX ? Collections.singletonList(myMPXOptions)
        : Collections.<GPOptionGroup> emptyList();
  }

  @Override
  public Component getCustomOptionsUI() {
    return null;
  }

  @Override
  public String getFileNamePattern() {
    return myFileFormatOption.getSelectedValue().getExtension();
  }

  @Override
  protected void setFormat(String format) {
    MsProjectFileFormat.getEntries().stream().filter( f -> f.getExtension().equalsIgnoreCase(format))
      .findFirst().ifPresent(myFileFormatOption::setSelectedValue);
  }

  @Override
  protected ExporterJob[] createJobs(final File outputFile, List<File> resultFiles) {
    ExporterJob job = createExportJob(outputFile);
    resultFiles.add(outputFile);
    return new ExporterJob[] {  job };
  }

  private ExporterJob createExportJob(final File outputFile) {
    return new ExporterJob("Export project") {
      @Override
      protected IStatus run() {
        ProjectFile outProject;
        try {
          outProject = new ProjectFileExporter(getProject()).run();
          ProjectWriter writer = createProjectWriter();
          writer.write(outProject, outputFile);
        } catch (MPXJException e) {
          getUIFacade().showErrorDialog(e);
          return Status.CANCEL_STATUS;
        } catch (IOException e) {
          getUIFacade().showErrorDialog(e);
          return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
      }
    };
  }

  private ProjectWriter createProjectWriter() {
    if (myFileFormatOption.getSelectedValue() == MsProjectFileFormat.MPX) {
      MPXWriter result = new MPXWriter();
      if (myLanguageOption.getSelectedValue() != null) {
        result.setLocale(myLanguageOption.getSelectedValue());
      }
      return result;
    }
    if (myFileFormatOption.getSelectedValue() == MsProjectFileFormat.MSPDI) {
      return new MSPDIWriter();
    }
    assert false : "Should not be here";
    return null;
  }

  @Override
  public String proposeFileExtension() {
    return getSelectedFormatExtension();
  }

  private String getSelectedFormatExtension() {
    return myFileFormatOption.getSelectedValue().getExtension();
  }

  @Override
  public String[] getFileExtensions() {
    return MsProjectFileFormat.getEntries().stream().map(MsProjectFileFormat::getExtension).toArray(String[]::new);
  }
}
