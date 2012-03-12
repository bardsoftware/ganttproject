/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.gui.options;

import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.AbstractPagesDialog;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.plugins.PluginManager;

public class SettingsDialog2 extends AbstractPagesDialog {
  private static List<OptionPageProvider> ourProviders;
  private final List<OptionPageProvider> myProviders = new ArrayList<OptionPageProvider>();

  static {
    ourProviders = PluginManager.getExtensions("net.sourceforge.ganttproject.OptionPageProvider",
        OptionPageProvider.class);
  }

  public SettingsDialog2(IGanttProject project, UIFacade uifacade, String pageOrderKey) {
    super("settings.app", uifacade, getPages(pageOrderKey, project, uifacade));
    for (OptionPageProvider p : ourProviders) {
      if (isPageVisible(p.getPageID())) {
        p.init(project, uifacade);
        myProviders.add(p);
      }
    }
  }

  @Override
  protected void onOk() {
    for (OptionPageProvider p : myProviders) {
      p.commit();
    }
  }

  private static List<ListItem> getPages(String pageOrderKey, IGanttProject project, UIFacade uiFacade) {
    return getListItems(ourProviders, pageOrderKey, project, uiFacade);
  }

  private static List<ListItem> getListItems(List<OptionPageProvider> providers, String pageOrderKey,
      IGanttProject project, UIFacade uiFacade) {
    Map<String, OptionPageProvider> pageId_provider = new HashMap<String, OptionPageProvider>();
    for (OptionPageProvider p : providers) {
      pageId_provider.put(p.getPageID(), p);
    }
    List<ListItem> items = new ArrayList<ListItem>();
    String[] listConfig = GanttLanguage.getInstance().getText(pageOrderKey).split(",");
    for (String pageName : listConfig) {
      ListItem li;
      if (pageName.startsWith("pageGroup.")) {
        li = new ListItem(true, pageName, GanttLanguage.getInstance().correctLabel(
            GanttLanguage.getInstance().getText(pageName)), null);
      } else {
        OptionPageProvider p = pageId_provider.get(pageName);
        assert p != null : "OptionPageProvider with pageID=" + pageName + " not found";
        li = new ListItem(false, p.getPageID(), p.toString(), (Container) new OptionPageProviderPanel(p, project,
            uiFacade).getComponent());
      }
      items.add(li);
    }
    return items;
  }
}