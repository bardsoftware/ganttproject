/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Alexandre Thomas, Dmitry Barashev

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
package biz.ganttproject.core.chart.canvas;


/**
 * Interface providing paint methods
 * 
 * @author bard
 */
public interface Painter {
  /** Initializes the graphic area (Call before painting!) */
  void prePaint();

  void paint(Canvas.Rectangle rectangle);

  void paint(Canvas.Line line);

  void paint(Canvas.Text next);

  void paint(Canvas.TextGroup textGroup);

  void paint(Canvas.Polygon p);
}
