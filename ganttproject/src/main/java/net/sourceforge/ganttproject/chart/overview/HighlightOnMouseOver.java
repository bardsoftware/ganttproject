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
package net.sourceforge.ganttproject.chart.overview;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.UIManager;

class HighlightOnMouseOver extends MouseAdapter {
  static final Color backgroundColor = UIManager.getColor("MenuItem.selectionBackground");
  static final String backgroundString = "#" + Integer.toHexString(backgroundColor.getRed())
      + Integer.toHexString(backgroundColor.getGreen()) + Integer.toHexString(backgroundColor.getBlue());
  private AbstractButton myComponent;
  private Action myActionOnClick;

  HighlightOnMouseOver(AbstractButton component, Action onClick) {
    myComponent = component;
    myActionOnClick = onClick;
  }

  @Override
  public void mouseEntered(MouseEvent arg0) {
    if (myComponent.isEnabled()) {
      myComponent.setBorderPainted(true);
    }
  }

  @Override
  public void mouseExited(MouseEvent arg0) {
    myComponent.setBorderPainted(false);
  }

  @Override
  public void mouseClicked(MouseEvent arg0) {
    if (myActionOnClick != null) {
      myActionOnClick.actionPerformed(null);
    }
  }
}