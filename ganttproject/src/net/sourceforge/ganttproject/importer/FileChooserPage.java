/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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
package net.sourceforge.ganttproject.importer;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.osgi.service.prefs.Preferences;

import com.google.common.base.Objects;

import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.filter.ExtensionBasedFileFilter;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.wizard.AbstractFileChooserPage;
import net.sourceforge.ganttproject.wizard.WizardPage;

/**
 * @author bard
 */
class FileChooserPage extends AbstractFileChooserPage {

  private final Importer myImporter;
  private File myFile;

  public FileChooserPage(UIFacade uiFacade, Importer importer, Preferences prefs) {
    super(uiFacade, prefs, GanttLanguage.getInstance().getText("importerFileChooserPageTitle"), createFileFilter(importer), createOptions(importer), false);
    myImporter = importer;
  }


  @Override
  protected int getFileChooserSelectionMode() {
    return JFileChooser.FILES_ONLY;
  }

  private static FileFilter createFileFilter(Importer importer) {
    return new ExtensionBasedFileFilter(importer.getFileNamePattern(), importer.getFileTypeDescription());
  }

  private static GPOptionGroup[] createOptions(Importer importer) {
    return importer.getSecondaryOptions();
  }

  @Override
  public String getTitle() {
    return GanttLanguage.getInstance().getText("importerFileChooserPageTitle");
  }

  @Override
  protected void setFile(File file) {
    if (Objects.equal(file, myFile)) {
      return;
    }
    myImporter.setFile(file);
    WizardPage importerPage = myImporter.getCustomPage();
    if (importerPage != null) {
      getWizard().setNextPage(importerPage);
    }
    if (myImporter.isReady()) {
      getWizard().setOkAction(new Runnable() {
        @Override
        public void run() {
          myImporter.run();
        }
      });
    } else {
      getWizard().setOkAction(null);
    }
    myFile = file;
  }
}
