/*
GanttProject is an opensource project management tool.
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
package net.sourceforge.ganttproject.gui.view;

import biz.ganttproject.app.View;
import javafx.scene.Node;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartSelection;
import org.jetbrains.annotations.NotNull;

/**
 * @author bard
 */
public interface GPViewManager {
  void createView(ViewProvider view);

  GPAction getCopyAction();

  GPAction getCutAction();

  GPAction getPasteAction();

  @NotNull GPAction getPropertiesAction();

  GPAction getDeleteAction();

  ChartSelection getSelectedArtefacts();

  View getActiveView();

  Chart getActiveChart();

  void activateNextView();

  void activatePrevView();

  Node getFxComponent();

  void onViewCreated(Runnable callback);

  void refresh();

  View getView(String id);

  void init(ViewProvider... viewProviders);
}
