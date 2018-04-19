/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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

import net.sourceforge.ganttproject.ChartComponentBase;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.ChartModelBase.ScrollingSession;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class ScrollViewInteraction extends MouseInteractionBase implements MouseInteraction {
  private final double myWheelFactor = Double.parseDouble(GPAction.getKeyStrokeText("mouse.wheel.factor"));
  private int myCurY;
  private int myCurX;
  private ScrollingSession myScrollingSession;
  private Component myComponent;

  public ScrollViewInteraction(MouseEvent e, TimelineFacade timelineFacade) {
    super(timelineFacade.getDateAt(0), timelineFacade);
    myComponent = e.getComponent();
    e.getComponent().setCursor(ChartComponentBase.CURSOR_DRAG);
    myScrollingSession = timelineFacade.createScrollingSession(e.getX(), e.getY());
    myCurX = e.getX();
    myCurY = e.getY();
  }

  @Override
  public void apply(MouseEvent event) {
    if (event instanceof MouseWheelEvent) {
      MouseWheelEvent wheelEvent = (MouseWheelEvent) event;
      int scrollIncrement = (int)(
          Math.max(wheelEvent.getScrollAmount(), 10)
              * (wheelEvent.getWheelRotation() < 0 ? myWheelFactor : -myWheelFactor)
      );
      if ((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
        // Vertical scroll
        myCurY += scrollIncrement;
      } else {
        myCurX += scrollIncrement;
      }
    } else {
      myCurX = event.getX();
      myCurY = event.getY();
    }
    myScrollingSession.scrollTo(myCurX, myCurY);
  }

  @Override
  public void finish() {
    myScrollingSession.finish();
    myComponent.setCursor(ChartComponentBase.DEFAULT_CURSOR);
  }
}
