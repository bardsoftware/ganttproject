/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

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
package net.sourceforge.ganttproject.gui;

import biz.ganttproject.app.InternationalizationKt;
import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.GPOptionGroup;
import javafx.beans.property.SimpleObjectProperty;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.projectwizard.WizardPage;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.Preferences;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class FileChooserPageBase implements WizardPage {
  protected static final String PREF_SELECTED_FILE = "selected_file";
  private final Document myDocument;

  private final TextFieldAndFileChooserComponent myChooser;
  private final OptionsPageBuilder myOptionsBuilder;
  private final JPanel mySecondaryOptionsComponent;
  private final JLabel myFileLabel = new JLabel("");
  private final BooleanOption myOverwriteOption = new DefaultBooleanOption("overwrite");

  private final Preferences myPreferences;

  public SimpleObjectProperty selectedUrlProperty = new SimpleObjectProperty<URL>();
  protected FileChooserPageBase(Preferences prefs, Document document, UIFacade uiFacade) {
    myPreferences = prefs;
    myDocument = document;
    myOptionsBuilder = new OptionsPageBuilder();
    mySecondaryOptionsComponent = new JPanel(new BorderLayout());
    mySecondaryOptionsComponent.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
    myOptionsBuilder.setI18N(new OptionsPageBuilder.I18N() {
      @Override
      protected boolean hasValue(String key) {
        return (key.equals(getCanonicalOptionLabelKey(myOverwriteOption) + ".trailing")) ? true : super.hasValue(key);
      }

      @Override
      protected String getValue(String key) {
        if (key.equals(getCanonicalOptionLabelKey(myOverwriteOption) + ".trailing")) {
          return InternationalizationKt.getRootLocalizer().formatText("document.overwrite");
        }
        return super.getValue(key);
      }
    });
    myChooser = new TextFieldAndFileChooserComponent(uiFacade, getFileChooserTitle()) {
      @Override
      protected void onFileChosen(File file) {
        tryChosenFile(file);
      }
    };
  }

  protected abstract String getFileChooserTitle();

  protected BooleanOption getOverwriteOption() {
    return myOverwriteOption;
  }
  /** @return a default export filename */
  protected String getDefaultFileName() {
    if (myDocument == null) {
      return "document.gan";
    }
    return myDocument.getFileName();
  }

  protected int getFileChooserSelectionMode() {
    return JFileChooser.FILES_AND_DIRECTORIES;
  }

  protected void tryChosenFile(File file) {
    IStatus status = FileChooserPageBase.this.onSelectedFileChange(file);
    if (!status.isOK()) {
      onSelectedUrlChange(null);
    }
    FileChooserPageBase.setStatus(myFileLabel, status);
  }

  @Override
  public Component getComponent() {
      JPanel myComponent = new JPanel(new BorderLayout());
    myChooser.setFileSelectionMode(getFileChooserSelectionMode());
    JComponent contentPanel = new JPanel(new BorderLayout());
    Box fileBox = Box.createVerticalBox();
    myChooser.setAlignmentX(Component.LEFT_ALIGNMENT);
    fileBox.add(myChooser);
    myFileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    fileBox.add(myFileLabel);
    fileBox.add(myOptionsBuilder.createOptionComponent(new GPOptionGroup("exporter", myOverwriteOption), myOverwriteOption));
    contentPanel.add(fileBox, BorderLayout.NORTH);
    contentPanel.add(mySecondaryOptionsComponent, BorderLayout.CENTER);
    myComponent.add(contentPanel, BorderLayout.NORTH);
    return myComponent;
  }

  protected void loadPreferences() {
    String oldFile = myPreferences.get(FileChooserPageBase.PREF_SELECTED_FILE, null);
    if (oldFile != null) {
      // Use the previously used path with the current filename for the default
      // name
      // The implementing classes can modify the file extension when desired
      String oldPath = new File(oldFile).getParent();
      File f = new File(oldPath, getDefaultFileName());
      myChooser.setFile(f);
    }
  }

  @Override
  public void setActive(boolean b) {
    GPOptionGroup[] optionGroups = getOptionGroups();
    if (b == false) {
        for (GPOptionGroup optionGroup : optionGroups) {
            optionGroup.commit();
        }
      if (myChooser.getFile() != null) {
        myPreferences.put(FileChooserPageBase.PREF_SELECTED_FILE, myChooser.getFile().getAbsolutePath());
      }
    } else {
        for (GPOptionGroup optionGroup : optionGroups) {
            optionGroup.lock();
        }
      if (mySecondaryOptionsComponent != null) {
        mySecondaryOptionsComponent.removeAll();
      }
      mySecondaryOptionsComponent.add(createSecondaryOptionsPanel(), BorderLayout.NORTH);
      myChooser.setFileFilter(createFileFilter());
      loadPreferences();
      onSelectedUrlChange(getSelectedUrl());
      //myWizard.getDialog().layout();
    }
  }

  protected Component createSecondaryOptionsPanel() {
    return myOptionsBuilder.buildPlanePage(getOptionGroups());
  }

  protected final Preferences getPreferences() {
    return myPreferences;
  }

  protected final TextFieldAndFileChooserComponent getChooser() {
    return myChooser;
  }

  private URL getSelectedUrl() {
     return myChooser.getSelectedURL();
  }

  protected abstract FileFilter createFileFilter();

  protected abstract GPOptionGroup[] getOptionGroups();

  protected void onSelectedUrlChange(URL selectedUrl) {
    selectedUrlProperty.set(selectedUrl);
  }

  protected IStatus setSelectedFile(File file) {
    try {
      onSelectedUrlChange(file.toURI().toURL());
      return new Status(IStatus.OK, "foo", IStatus.OK, "  ", null);
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return new Status(IStatus.ERROR, "foo", IStatus.ERROR, e.getMessage(), null);
    }
  }

  protected IStatus onSelectedFileChange(File file) {
    if (file == null) {
      return new Status(IStatus.ERROR, "foo", IStatus.ERROR, "File does not exist", null);
    }
    if (!file.exists()) {
      return new Status(IStatus.ERROR, "foo", IStatus.ERROR, "File does not exist", null);
    }
    if (!file.canRead()) {
      return new Status(IStatus.ERROR, "foo", IStatus.ERROR, "File read error", null);
    }
    return setSelectedFile(file);
  }

  private static void setStatus(JLabel label, IStatus status) {
    label.setOpaque(true);
    if (status.isOK()) {
      UIUtil.clearErrorLabel(label);
      label.setText(status.getMessage());
    } else {
      UIUtil.setupErrorLabel(label, status.getMessage());
    }
  }
}
