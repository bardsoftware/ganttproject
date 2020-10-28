/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package org.ganttproject.impex.htmlpdf;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.ganttproject.impex.htmlpdf.itext.ITextEngine;
import org.ganttproject.impex.htmlpdf.itext.ITextStylesheet;

import biz.ganttproject.core.option.GPOptionGroup;


public class ExporterToPDF extends StylesheetExporterBase {

  private final ITextEngine myITextEngine = new ITextEngine(this);
  private Stylesheet mySelectedStylesheet;

  @Override
  protected ExporterJob[] createJobs(File outputFile, List<File> resultFiles) {
    super.setCommandLineStylesheet();

    if (mySelectedStylesheet instanceof ITextStylesheet) {
      resultFiles.add(outputFile);
      return myITextEngine.createJobs(outputFile, resultFiles);
    }
    assert false : "Unknown stylesheet is selected: " + mySelectedStylesheet;
    return null;
  }

  @Override
  protected String getStylesheetOptionID() {
    return "impex.pdf.stylesheet";
  }

  @Override
  protected List<Stylesheet> getStylesheets() {
    List<Stylesheet> result = new ArrayList<Stylesheet>();
    result.addAll(myITextEngine.getStylesheets());
    return result;
  }

  @Override
  protected void setSelectedStylesheet(Stylesheet stylesheet) {
    mySelectedStylesheet = stylesheet;
    initEngine();
  }

  private void initEngine() {
    if (mySelectedStylesheet instanceof ITextStylesheet) {
      myITextEngine.setContext(getProject(), getUIFacade(), getPreferences(), mySelectedStylesheet);
    } else {
      assert false : "Unknown stylesheet is selected: " + mySelectedStylesheet;
    }
  }

  @Override
  public String getFileTypeDescription() {
    return language.getText("impex.pdf.description");
  }

  @Override
  public Component getCustomOptionsUI() {
    if (mySelectedStylesheet instanceof ITextStylesheet) {
      return myITextEngine.getCustomOptionsUI();
    }
    return null;
  }

  @Override
  public List<GPOptionGroup> getSecondaryOptions() {
    List<GPOptionGroup> result = new ArrayList<GPOptionGroup>();
    result.add(createExportRangeOptionGroup());
    if (mySelectedStylesheet instanceof ITextStylesheet) {
      result.addAll(myITextEngine.getSecondaryOptions());
    }
    return result;
  }

  @Override
  public String getFileNamePattern() {
    return "pdf";
  }

  @Override
  public String proposeFileExtension() {
    return "pdf";
  }

  @Override
  public String[] getFileExtensions() {
    return new String[] { "pdf" };
  }

}
