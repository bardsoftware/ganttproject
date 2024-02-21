/*
GanttProject is an opensource project management tool. License: GPL3
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
package net.sourceforge.ganttproject.action.view;

import biz.ganttproject.core.option.FontOption;
import biz.ganttproject.core.option.IntegerOption;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.ViewToggleAction;
import net.sourceforge.ganttproject.gui.view.GPViewManager;
import net.sourceforge.ganttproject.gui.view.ViewProvider;
import net.sourceforge.ganttproject.plugins.PluginManager;

import javax.swing.*;
import java.util.List;

/**
 * Collection of actions present in the view menu
 */
public class ViewMenu extends JMenu {
  public ViewMenu(final IGanttProject project, GPViewManager viewManager, IntegerOption dpiOption, FontOption chartFontOption, String key) {
    super(GPAction.createVoidAction(key));

    List<ViewProvider> charts = PluginManager.getViewProviders();
    if (charts.isEmpty()) {
      setEnabled(false);
    }
    for (ViewProvider viewProvider : charts) {
      var action = new ViewToggleAction(viewManager, viewProvider);
      action.updateAction();
      add(new JCheckBoxMenuItem(action));
    }
    setToolTipText(null);
  }
}
