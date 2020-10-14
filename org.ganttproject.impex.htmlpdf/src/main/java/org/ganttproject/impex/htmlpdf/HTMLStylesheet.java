/*
 * Created on 25.08.2005
 */
package org.ganttproject.impex.htmlpdf;

import java.io.File;

import javax.xml.transform.sax.TransformerHandler;

public interface HTMLStylesheet extends Stylesheet {
  interface InputVersion {
    String GP1X = "ganttproject-1.x";
  }

  String getInputVersion();

  TransformerHandler createTitlePageHandler();

  @Override
  String getLocalizedName();

  TransformerHandler createTasksPageHandler();

  TransformerHandler createGanttChartPageHandler();

  TransformerHandler createResourcesPageHandler();

  File getImagesDirectory();
}
