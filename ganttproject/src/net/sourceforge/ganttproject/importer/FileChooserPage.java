/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import java.net.URL;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.osgi.service.prefs.Preferences;

import net.sourceforge.ganttproject.filter.ExtensionBasedFileFilter;
import net.sourceforge.ganttproject.gui.FileChooserPageBase;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.projectwizard.WizardImpl;
import net.sourceforge.ganttproject.importer.ImportFileWizardImpl.State;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
class FileChooserPage extends FileChooserPageBase {

    private final State myState;

    public FileChooserPage(WizardImpl wizardImpl, Preferences prefs, State state) {
        super(wizardImpl, prefs, false);
        myState = state;
    }

    @Override
    protected String getFileChooserTitle() {
        return GanttLanguage.getInstance().getText("importerFileChooserPageTitle");
    }

    @Override
    protected int getFileChooserSelectionMode() {
        return JFileChooser.FILES_ONLY;
    }

    public String getTitle() {
        return GanttLanguage.getInstance().getText("importerFileChooserPageTitle");
    }

    @Override
    protected FileFilter createFileFilter() {
        return new ExtensionBasedFileFilter(
                myState.myImporter.getFileNamePattern(), myState.myImporter.getFileTypeDescription());
    }

    @Override
    protected GPOptionGroup[] getOptionGroups() {
        return myState.myImporter==null ? new GPOptionGroup[0] : myState.myImporter.getSecondaryOptions();
    }

    @Override
    protected void onSelectedUrlChange(URL selectedUrl) {
        myState.setUrl(selectedUrl);
        super.onSelectedUrlChange(selectedUrl);
    }
}
