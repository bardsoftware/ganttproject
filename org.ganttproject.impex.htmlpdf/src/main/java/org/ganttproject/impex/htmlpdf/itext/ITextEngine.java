/*
GanttProject is an opensource project management tool.
Copyright (C) 2009-2012 Dmitry Barashev, GanttProject Team

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

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.export.ExporterBase;
import net.sourceforge.ganttproject.export.ExportException;
import net.sourceforge.ganttproject.export.ExporterBase.ExporterJob;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.ganttproject.impex.htmlpdf.AbstractEngine;
import org.ganttproject.impex.htmlpdf.ExporterToPDF;
import org.ganttproject.impex.htmlpdf.Stylesheet;
import org.ganttproject.impex.htmlpdf.StylesheetFactoryImpl;
import org.ganttproject.impex.htmlpdf.fonts.TTFontCache;
import org.osgi.service.prefs.Preferences;

import biz.ganttproject.core.option.GPOptionGroup;

public class ITextEngine extends AbstractEngine {
  private ITextStylesheet myStylesheet;
  private final TTFontCache myFontCache;
  private FontSubstitutionModel mySubstitutionModel;
  private Object myFontsMutex = new Object();
  private boolean myFontsReady = false;
  private ExporterToPDF myExporter;

  public ITextEngine(ExporterToPDF exporter) {
    myExporter = exporter;
    myFontCache = new TTFontCache();
    registerFonts();
  }

  public List<GPOptionGroup> getSecondaryOptions() {
    return Arrays.asList(getSecondaryOptionsArray());
  }

  private GPOptionGroup[] getSecondaryOptionsArray() {
    return ((ThemeImpl) myStylesheet).getOptions();
  }

  public Component getCustomOptionsUI() {
    waitRegisterFonts();
    JPanel result = new JPanel(new BorderLayout());
    result.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
    OptionsPageBuilder builder = new OptionsPageBuilder();

    List<GPOptionGroup> options = new ArrayList<GPOptionGroup>();
    options.addAll(myExporter.getSecondaryOptions());
    result.add(builder.buildPlanePage(options.toArray(new GPOptionGroup[0])), BorderLayout.NORTH);
    result.add(createFontPanel(), BorderLayout.CENTER);
    return result;
  }

  public String[] getCommandLineKeys() {
    return new String[] { "itext" };
  }

  private Component createFontPanel() {
    return new FontSubstitutionPanel(mySubstitutionModel).getComponent();
  }

  public void setContext(IGanttProject project, UIFacade uiFacade, Preferences preferences, Stylesheet stylesheet) {
    super.setContext(project, uiFacade, preferences);
    setSelectedStylesheet(stylesheet);
  }

  public void setSelectedStylesheet(Stylesheet stylesheet) {
    waitRegisterFonts();
    myStylesheet = (ITextStylesheet) stylesheet;
    if (getPreferences() != null) {
      Preferences node = getPreferences().node("/configuration/org.ganttproject.impex.htmlpdf/font-substitution");
      mySubstitutionModel = new FontSubstitutionModel(myFontCache, myStylesheet, node);
      myStylesheet.setFontSubstitutionModel(mySubstitutionModel);
    }
  }

  public void setStylesheet(Stylesheet stylesheet) {
    myStylesheet = (ITextStylesheet) stylesheet;
  }

  public List<Stylesheet> getStylesheets() {
    StylesheetFactoryImpl factory = new StylesheetFactoryImpl() {
      @Override
      protected Stylesheet newStylesheet(URL resolvedUrl, String localizedName) {
        return new ThemeImpl(resolvedUrl, localizedName, getExporter(), myFontCache);
      }
    };
    return factory.createStylesheets(ITextStylesheet.class);
  }

  private ExporterBase getExporter() {
    return myExporter;
  }

  private void registerFonts() {
    Thread fontReadingThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          // Random waiting seems silly, depending on the available
          // resources (CPU speed, number of processes running etc)
          // this might take longer or shorter...
          // FIXME Add some better way of determining whether the fonts can be
          // read already
          Thread.sleep(10000);
          GPLogger.getLogger(TTFontCache.class).info("Scanning font directories...");
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          GPLogger.logToLogger(e);
        }
        registerFontDirectories();
        synchronized (ITextEngine.this.myFontsMutex) {
          myFontsReady = true;
          myFontsMutex.notifyAll();
        }
        GPLogger.getLogger(TTFontCache.class).info("Scanning font directories completed");
      }
    });
    fontReadingThread.setPriority(Thread.MIN_PRIORITY);
    fontReadingThread.start();
  }

  private void waitRegisterFonts() {
    while (myFontsMutex != null) {
      synchronized (myFontsMutex) {
        if (myFontsReady) {
          break;
        }
        try {
          myFontsMutex.wait();
        } catch (InterruptedException e) {
          GPLogger.log(e);
          break;
        }
      }
    }
  }

  protected void registerFontDirectories() {
    myFontCache.registerDirectory(System.getProperty("java.home") + "/lib/fonts");
    IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
    IConfigurationElement[] configElements = extensionRegistry.getConfigurationElementsFor("org.ganttproject.impex.htmlpdf.FontDirectory");
    for (int i = 0; i < configElements.length; i++) {
      final String dirName = configElements[i].getAttribute("name");
      if (Boolean.TRUE.toString().equalsIgnoreCase(configElements[i].getAttribute("absolute"))) {
        myFontCache.registerDirectory(dirName);
      } else {
        String namespace = configElements[i].getDeclaringExtension().getNamespaceIdentifier();
        URL dirUrl = Platform.getBundle(namespace).getResource(dirName);
        if (dirUrl == null) {
          GPLogger.getLogger(ITextEngine.class).warning(
              "Failed to find directory " + dirName + " in plugin " + namespace);
          continue;
        }
        try {
          URL resolvedDir = Platform.resolve(dirUrl);
          myFontCache.registerDirectory(resolvedDir.getPath());
        } catch (IOException e) {
          GPLogger.getLogger(ITextEngine.class).log(Level.WARNING, e.getMessage(), e);
          continue;
        }
      }
    }
  }

  public ExporterJob[] createJobs(File outputFile, List<File> resultFiles) {
    waitRegisterFonts();
    return new ExporterJob[] { createTransformationJob(outputFile) };
  }

  private ExporterJob createTransformationJob(final File outputFile) {
    ExporterJob result = new ExporterJob("Generating PDF") {
      @Override
      protected IStatus run() {
        assert myStylesheet != null;
        OutputStream out = null;
        try {
          out = new FileOutputStream(outputFile);
          ((ThemeImpl) myStylesheet).run(getProject(), getUiFacade(), out);
        } catch (ExportException e) {
          throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        } finally {
        }
        return Status.OK_STATUS;
      }
    };
    return result;
  }
}
