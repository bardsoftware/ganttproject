/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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
package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.chart.canvas.TextMetrics;

import java.awt.*;

/**
 * Simple text length calculator which considers all characters occupying a square block
 * of the same size.
 *
 *  @author dbarashev (Dmitry Barashev)
 */
public class TestTextLengthCalculator implements TextMetrics {
  private final int myBboxSize;

  public TestTextLengthCalculator(int bboxSize) {
    myBboxSize = bboxSize;
  }

  @Override
  public int getTextLength(String text) {
    return text.length() * myBboxSize;
  }

  @Override
  public int getTextHeight(String text) {
    return myBboxSize;
  }

  @Override
  public Object getState() {
    return Boolean.TRUE;
  }

  @Override
  public int getTextHeight(Font f, String text) {
    return getTextHeight(text);
  }
}
