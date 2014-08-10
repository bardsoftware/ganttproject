/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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
package org.ganttproject.impex.htmlpdf;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.export.ExportException;
import net.sourceforge.ganttproject.util.FileUtil;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.xml.sax.SAXException;

import biz.ganttproject.core.option.GPOptionGroup;

public class ExporterToHTML extends StylesheetExporterBase {
  static final String GANTT_CHART_FILE_EXTENSION = "png";
  static final String RESOURCE_CHART_FILE_EXTENSION = "res.png";
  private static final String PNG_FORMAT_NAME = "png";
  private HTMLStylesheet mySelectedStylesheet;
  private final HtmlSerializer mySerializer = new HtmlSerializer(this);

  @Override
  public String getFileTypeDescription() {
    return language.getText("impex.html.description");
  }

  @Override
  protected void setSelectedStylesheet(Stylesheet stylesheet) {
    mySelectedStylesheet = (HTMLStylesheet) stylesheet;
  }

  @Override
  public List<GPOptionGroup> getSecondaryOptions() {
    return null;
  }

  @Override
  public String getFileNamePattern() {
    return "html";
  }

  @Override
  protected ExporterJob[] createJobs(File outputFile, List<File> resultFiles) {
    super.setCommandLineStylesheet();

    ExporterJob generateGanttChartJob = createGenerateGanttChartJob(outputFile, resultFiles);
    ExporterJob generateResourceChartJob = createGenerateResourceChartJob(outputFile, resultFiles);
    ExporterJob generatePagesJob = createGeneratePagesJob(outputFile, resultFiles);
    ExporterJob copyImagesJob = createCopyImagesJob(outputFile, resultFiles);
    return new ExporterJob[] { generateGanttChartJob, generateResourceChartJob, generatePagesJob, copyImagesJob };
  }

  private ExporterJob createGenerateGanttChartJob(final File outputFile, final List<File> resultFiles) {
    ExporterJob result = new ExporterJob("generate gantt chart") {
      @Override
      protected IStatus run() {
        try {
          RenderedImage ganttChartImage = getGanttChart().getRenderedImage(
              createExportSettings());
          File ganttChartImageFile;
          ganttChartImageFile = replaceExtension(outputFile, GANTT_CHART_FILE_EXTENSION);
          ImageIO.write(ganttChartImage, PNG_FORMAT_NAME, ganttChartImageFile);
          resultFiles.add(ganttChartImageFile);
        } catch (IOException e) {
          getUIFacade().showErrorDialog(e);
          return Status.CANCEL_STATUS;
        } catch (OutOfMemoryError e) {
          getUIFacade().showErrorDialog(new RuntimeException("Out of memory when creating Gantt chart image", e));
          return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
      }
    };
    return result;
  }

  private ExporterJob createGenerateResourceChartJob(final File outputFile, final List<File> resultFiles) {
    ExporterJob result = new ExporterJob("Generate resource chart") {
      @Override
      protected IStatus run() {
        try {
          RenderedImage resourceChartImage = getResourceChart().getRenderedImage(
              createExportSettings());
          File resourceChartImageFile = replaceExtension(outputFile, RESOURCE_CHART_FILE_EXTENSION);
          ImageIO.write(resourceChartImage, PNG_FORMAT_NAME, resourceChartImageFile);
          resultFiles.add(resourceChartImageFile);
        } catch (IOException e) {
          getUIFacade().showErrorDialog(e);
          return Status.CANCEL_STATUS;
        } catch (OutOfMemoryError e) {
          getUIFacade().showErrorDialog(new RuntimeException("Out of memory when creating resource chart image", e));
          return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
      }
    };
    return result;
  }

  private ExporterJob createGeneratePagesJob(final File outputFile, final List<File> resultFiles) {
    ExporterJob result = new ExporterJob("Generate HTML pages") {

      @Override
      protected IStatus run() {
        try {
          {
            TransformerHandler handler = mySelectedStylesheet.createTitlePageHandler();
            handler.setResult(new StreamResult(outputFile));
            mySerializer.serialize(handler, outputFile);
            resultFiles.add(outputFile);
          }
          {
            TransformerHandler handler = mySelectedStylesheet.createTasksPageHandler();
            File tasksPageFile = appendSuffixBeforeExtension(outputFile, "-tasks");
            handler.setResult(new StreamResult(tasksPageFile));
            mySerializer.serialize(handler, outputFile);
            resultFiles.add(tasksPageFile);
          }
          {
            TransformerHandler handler = mySelectedStylesheet.createGanttChartPageHandler();
            File chartPageFile = appendSuffixBeforeExtension(outputFile, "-chart");
            handler.setResult(new StreamResult(chartPageFile));
            mySerializer.serialize(handler, outputFile);
            resultFiles.add(chartPageFile);
          }
          {
            TransformerHandler handler = mySelectedStylesheet.createResourcesPageHandler();
            File resourcesPageFile = appendSuffixBeforeExtension(outputFile, "-resources");
            handler.setResult(new StreamResult(resourcesPageFile));
            mySerializer.serialize(handler, outputFile);
            resultFiles.add(resourcesPageFile);
          }
        } catch (SAXException e) {
          getUIFacade().showErrorDialog(e);
          return Status.CANCEL_STATUS;
        } catch (IOException e) {
          getUIFacade().showErrorDialog(e);
          return Status.CANCEL_STATUS;
        } catch (OutOfMemoryError e) {
          getUIFacade().showErrorDialog(new RuntimeException("Out of memory when running XSL transformation", e));
          return Status.CANCEL_STATUS;
        } catch (ExportException e) {
          getUIFacade().showErrorDialog(e);
          return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
      }
    };
    return result;
  }

  private ExporterJob createCopyImagesJob(final File outputFile, final List<File> resultFiles) {
    ExporterJob result = new ExporterJob("Copying images") {
      @Override
      protected IStatus run() {
        try {
          File imagesDir = mySelectedStylesheet.getImagesDirectory();
          if (imagesDir != null && imagesDir.isDirectory() && imagesDir.exists()) {
            File[] lof = imagesDir.listFiles();
            if (lof.length != 0) {
              File resultImagesDir = new File(outputFile.getParentFile(), imagesDir.getName());
              if (resultImagesDir.mkdir()) {
                for (int i = 0; i < lof.length; i++) {
                  File nextInFile = lof[i];
                  if (nextInFile.isDirectory()) {
                    continue;
                  }
                  File outFile = new File(resultImagesDir, nextInFile.getName());
                  outFile.createNewFile();
                  FileInputStream inStream = new FileInputStream(nextInFile);
                  FileOutputStream outStream = new FileOutputStream(outFile);
                  byte[] buffer = new byte[(int) nextInFile.length()];
                  inStream.read(buffer);
                  outStream.write(buffer);
                }
              }
            }
          }
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
    return "html";
  }

  @Override
  public String[] getFileExtensions() {
    String s[] = { "html" };
    return s;
  }

  @Override
  protected String getStylesheetOptionID() {
    return "impex.html.stylesheet";
  }

  @Override
  protected List<Stylesheet> getStylesheets() {
    StylesheetFactoryImpl factory = new StylesheetFactoryImpl() {
      @Override
      protected Stylesheet newStylesheet(URL resolvedUrl, String localizedName) {
        return new HTMLStylesheetImpl(resolvedUrl, localizedName);
      }
    };
    return factory.createStylesheets(HTMLStylesheet.class);
  }

  class HTMLStylesheetImpl extends StylesheetImpl implements HTMLStylesheet {
    HTMLStylesheetImpl(URL stylesheetURL, String localizedName) {
      super(stylesheetURL, localizedName);
    }

    @Override
    public String getInputVersion() {
      return HTMLStylesheet.InputVersion.GP1X;
    }

    @Override
    public TransformerHandler createTitlePageHandler() {
      try {
        URL titleUrl = new URL(getUrl(), "gantt.xsl");
        TransformerHandler result = mySerializer.createHandler(titleUrl.toString());
        return result;
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      } catch (TransformerConfigurationException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public TransformerHandler createTasksPageHandler() {
      try {
        URL tasksUrl = new URL(getUrl(), "gantt-tasks.xsl");
        TransformerHandler result = mySerializer.createHandler(tasksUrl.toString());
        return result;
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      } catch (TransformerConfigurationException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public TransformerHandler createGanttChartPageHandler() {
      try {
        URL tasksUrl = new URL(getUrl(), "gantt-chart.xsl");
        TransformerHandler result = mySerializer.createHandler(tasksUrl.toString());
        return result;
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      } catch (TransformerConfigurationException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public TransformerHandler createResourcesPageHandler() {
      try {
        URL tasksUrl = new URL(getUrl(), "gantt-resources.xsl");
        TransformerHandler result = mySerializer.createHandler(tasksUrl.toString());
        return result;
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      } catch (TransformerConfigurationException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public File getImagesDirectory() {
      try {
        URL imagesUrl = new URL(getUrl(), "images");
        File result = new File(imagesUrl.getPath());
        return result;
      } catch (MalformedURLException e) {
        if (!GPLogger.log(e)) {
          e.printStackTrace(System.err);
        }
        throw new RuntimeException(e);
      }
    }
  }

  private static File appendSuffixBeforeExtension(File f, String suffix) throws IOException {
    return FileUtil.appendSuffixBeforeExtension(f, suffix);
  }

  static File replaceExtension(File f, String newExtension) throws IOException {
    File result = FileUtil.replaceExtension(f, newExtension);
    if (!result.exists()) {
      result.createNewFile();
    }
    return result;
  }

}
