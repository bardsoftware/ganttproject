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

import biz.ganttproject.FXUtil;
import biz.ganttproject.app.FXThread;
import biz.ganttproject.core.option.GPOptionGroup;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import kotlin.Unit;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.export.ExportException;
import net.sourceforge.ganttproject.export.ExporterBase;
import net.sourceforge.ganttproject.export.ExporterBase.ExporterJob;
import net.sourceforge.ganttproject.gui.UIFacade;
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
import org.jetbrains.annotations.Nullable;
import org.osgi.service.prefs.Preferences;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class ITextEngine extends AbstractEngine {
  private ITextStylesheet myStylesheet;
  private final TTFontCache myFontCache;
  private FontSubstitutionModel mySubstitutionModel;
  private ExporterToPDF myExporter;
  private final ExecutorService fontRegisterExecutor = Executors.newSingleThreadExecutor();
  private final CompletableFuture<Void> fontRegisterFuture = new CompletableFuture<>();

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
    return null;
  }

  public @Nullable Parent createCustomOptionsUiFx() {
    var borderPane = new BorderPane();
    borderPane.setCenter(new Label("Loading fonts..."));
    fontRegisterFuture.thenRun(() -> {
      mySubstitutionModel.init();
      var substitutionPanel = new FontSubstitutionPanel(mySubstitutionModel, new SimpleStringProperty("")).getComponentFx();
      FXThread.INSTANCE.runLater(() -> {
        FXUtil.INSTANCE.transitionCenterPane(borderPane, substitutionPanel, () -> Unit.INSTANCE);
        return Unit.INSTANCE;
      });
    });
    return borderPane;
  }

  public String[] getCommandLineKeys() {
    return new String[] { "itext" };
  }

  public void setContext(IGanttProject project, UIFacade uiFacade, Preferences preferences, Stylesheet stylesheet) {
    super.setContext(project, uiFacade, preferences);
    setSelectedStylesheet(stylesheet);
  }

  public void setSelectedStylesheet(Stylesheet stylesheet) {
    myStylesheet = (ITextStylesheet) stylesheet;
    if (getPreferences() != null) {
      Preferences node = getPreferences().node("/configuration/org.ganttproject.impex.htmlpdf/font-substitution");
      mySubstitutionModel = new FontSubstitutionModel(myFontCache, myStylesheet, node);
      mySubstitutionModel.init();
      myStylesheet.setFontSubstitutionModel(mySubstitutionModel);

      Preferences htmlPdfPrefs = getPreferences().node("/configuration/org.ganttproject.impex.htmlpdf");
      myStylesheet.setPrefs(htmlPdfPrefs);
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
    fontRegisterExecutor.submit(() -> {
      var logger = GPLogger.create("Export.Pdf.Fonts");
      try {
        // Random waiting seems silly, depending on the available
        // resources (CPU speed, number of processes running etc)
        // this might take longer or shorter...
        // FIXME Add some better way of determining whether the fonts can be
        // read already
        Thread.sleep(10000);
        logger.debug("Scanning font directories...");
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        GPLogger.logToLogger(e);
      }
      registerFontDirectories();
      logger.debug("Scanning font directories completed");
      fontRegisterFuture.complete(null);
    });
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
    try {
      fontRegisterFuture.get();
      return new ExporterJob[] { createTransformationJob(outputFile) };
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private ExporterJob createTransformationJob(final File outputFile) {
    ExporterJob result = new ExporterJob("Generating PDF") {
      @Override
      protected IStatus run() {
        assert myStylesheet != null;
        try(OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
          ((ThemeImpl) myStylesheet).run(getProject(), getUiFacade(), out);
        } catch (ExportException e) {
          throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return Status.OK_STATUS;
      }
    };
    return result;
  }

}
