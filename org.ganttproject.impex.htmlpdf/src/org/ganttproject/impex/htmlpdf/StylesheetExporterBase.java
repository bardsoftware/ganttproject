/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 Alexandre Thomas, Dmitry Barashev, GanttProject Team

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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.export.ExporterBase;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

import org.osgi.service.prefs.Preferences;

public abstract class StylesheetExporterBase extends ExporterBase {

  private GPOptionGroup myOptions;

  protected EnumerationOption createStylesheetOption(String optionID, final List<Stylesheet> stylesheets) {
    final List<String> names = new ArrayList<String>();
    for (Stylesheet s : stylesheets) {
      names.add(s.getLocalizedName());
    }
    EnumerationOption stylesheetOption = new DefaultEnumerationOption<Stylesheet>(optionID, names) {
      @Override
      public void commit() {
        super.commit();
        String value = getValue();
        int index = names.indexOf(value);
        if (index >= 0) {
          setSelectedStylesheet(stylesheets.get(index));
        }
      }
    };
    return stylesheetOption;
  }

  @Override
  public abstract String[] getFileExtensions();

  protected abstract List<Stylesheet> getStylesheets();

  protected abstract void setSelectedStylesheet(Stylesheet stylesheet);

  protected abstract String getStylesheetOptionID();

  public StylesheetExporterBase() {
  }

  @Override
  public Component getCustomOptionsUI() {
    return null;
  }

  @Override
  public void setContext(IGanttProject project, UIFacade uiFacade, Preferences prefs) {
    super.setContext(project, uiFacade, prefs);
    createStylesheetOption(getStylesheets());
  }

  private void createStylesheetOption(List<Stylesheet> stylesheets) {
    EnumerationOption stylesheetOption = createStylesheetOption(getStylesheetOptionID(), stylesheets);
    stylesheetOption.setValue(stylesheets.get(0).getLocalizedName());
    myOptions = new GPOptionGroup("exporter.html", new GPOption[] { stylesheetOption });
    myOptions.setTitled(false);
  }

  @Override
  public GPOptionGroup getOptions() {
    return myOptions;
  }
}
