/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.chart.mouse;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;

import net.sourceforge.ganttproject.AbstractChartImplementation;
import net.sourceforge.ganttproject.ChartComponentBase;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.util.MouseUtil;

public class MouseListenerBase extends MouseAdapter {
  private UIFacade myUiFacade;
  private ChartComponentBase myChartComponent;
  private AbstractChartImplementation myChartImplementation;

  protected MouseListenerBase(UIFacade uiFacade, ChartComponentBase chartComponent,
      AbstractChartImplementation chartImplementation) {
    assert uiFacade != null && chartComponent != null && chartImplementation != null;
    myUiFacade = uiFacade;
    myChartComponent = chartComponent;
    myChartImplementation = chartImplementation;
  }

  protected UIFacade getUIFacade() {
    return myUiFacade;
  }

  protected void showPopupMenu(MouseEvent e) {
    Action[] actions = getPopupMenuActions(e);
    if (actions.length > 0) {
      getUIFacade().showPopupMenu(myChartComponent, actions, e.getX(), e.getY());
    }
  }

  protected void startScrollView(MouseEvent e) {
    myChartImplementation.beginScrollViewInteraction(e);
    myChartComponent.requestFocus();
  }

  @Override
  public void mousePressed(MouseEvent e) {
    String text = MouseUtil.toString(e);
    if (e.isPopupTrigger() || text.equals(GPAction.getKeyStrokeText("mouse.contextMenu"))) {
      showPopupMenu(e);
      return;
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    super.mouseReleased(e);
    myChartImplementation.finishInteraction();
    myChartComponent.reset();
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    myChartComponent.setDefaultCursor();
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }

  protected Action[] getPopupMenuActions(MouseEvent e) {
    return new Action[0];
  }
}