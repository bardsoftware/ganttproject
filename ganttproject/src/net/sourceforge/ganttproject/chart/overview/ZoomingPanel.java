/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010-2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.chart.overview;

import com.google.common.base.Function;
import net.sourceforge.ganttproject.action.zoom.ZoomActionSet;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.UIFacade;

import javax.annotation.Nullable;
import java.awt.*;

/**
 * Creates a panel containing buttons of the zoom actions
 */
public class ZoomingPanel {
  private final TimelineChart myChart;
  private final UIFacade myUIFacade;

  public ZoomingPanel(UIFacade uiFacade, TimelineChart chart) {
    myChart = chart;
    myUIFacade = uiFacade;
  }

  public Component getComponent() {
    ZoomActionSet zoomActionSet = myUIFacade.getZoomActionSet();
    return new ToolbarBuilder()
        .withDpiOption(myUIFacade.getDpiOption())
        .withLafOption(myUIFacade.getLafOption(), new Function<String, Float>() {
          @Override
          public Float apply(@Nullable String s) {
            return (s.indexOf("nimbus") >= 0) ? 2f : 1f;
          }
        })
        .withGapFactory(ToolbarBuilder.Gaps.VDASH)
        .withBackground(myChart.getStyle().getSpanningHeaderBackgroundColor())
        .withHeight(24)
        .addButton(zoomActionSet.getZoomInAction())
        .addButton(zoomActionSet.getZoomOutAction())
        .build()
        .getToolbar();
  }
}