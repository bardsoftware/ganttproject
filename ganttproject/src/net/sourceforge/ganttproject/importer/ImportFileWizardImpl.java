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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttOptions;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.projectwizard.WizardImpl;
import net.sourceforge.ganttproject.plugins.PluginManager;

/**
 * @author bard
 */
public class ImportFileWizardImpl extends WizardImpl {
    private final State myState;

    private static List<Importer> ourImporters;

    public ImportFileWizardImpl(UIFacade uiFacade, IGanttProject project, GanttOptions options) {
        super(uiFacade, language.getText("importWizard.dialog.title"));
        myState = new State();
        if (ourImporters == null) {
            ourImporters = getImporters();
        }
        for (Importer importer : ourImporters) {
            importer.setContext(project, uiFacade, options.getPluginPreferences());
        }
        addPage(new ImporterChooserPage(ourImporters, myState));
        addPage(new FileChooserPage(
                this,
                options.getPluginPreferences().node("/instance/net.sourceforge.ganttproject/import"),
                myState));
    }

    private static List<Importer> getImporters() {
        return PluginManager.getExtensions(Importer.EXTENSION_POINT_ID, Importer.class);
    }

    @Override
    protected void onOkPressed() {
        super.onOkPressed();
        if ("file".equals(myState.getUrl().getProtocol())) {
            try {
                String path = URLDecoder.decode(myState.getUrl().getPath(), "utf-8");
                myState.myImporter.run(new File(path));
            } catch (UnsupportedEncodingException e) {
                GPLogger.log(e);
            }
        }
        else {
            getUIFacade().showErrorDialog(new Exception("You are not supposed to see this. Please report this bug."));
        }
    }

    @Override
    protected boolean canFinish() {
        return myState.myImporter != null
            && myState.getUrl() != null
            && "file".equals(myState.getUrl().getProtocol());
    }

    static class State {
        Importer myImporter;

        private URL myUrl;

        public void setUrl(URL url) {
            myUrl = url;
        }

        public URL getUrl() {
            return myUrl;
        }
    }
}
