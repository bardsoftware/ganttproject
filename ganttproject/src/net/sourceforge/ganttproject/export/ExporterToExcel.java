package net.sourceforge.ganttproject.export;

import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Created by valentin on 15.03.17.
 */
public class ExporterToExcel extends ExporterBase {

  private static String[] FILE_EXTENTION = new String[] { ".xlsx" };

  @Override
  public String getFileTypeDescription() {
    return GanttLanguage.getInstance().getText("");//add description in prorerty files and paste key here
  }

  @Override
  public GPOptionGroup getOptions() {
    return null;
  }

  @Override
  public List<GPOptionGroup> getSecondaryOptions() {
    return null;
  }

  @Override
  public String getFileNamePattern() { return FILE_EXTENTION[0]; }

  @Override
  public String proposeFileExtension() {
    return null;
  }

  @Override
  public String[] getFileExtensions() {
    return new String[0];
  }

  @Override
  public Component getCustomOptionsUI() {
    return null;
  }

  @Override
  protected ExporterJob[] createJobs(File outputFile, List<File> resultFiles) {
    return new ExporterJob[0];
  }
}
