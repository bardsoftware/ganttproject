/*
 *
 * Copyright 2017 Alexandr Kurutin, BarD Software s.r.o
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 * /
 */

package net.sourceforge.ganttproject.export;

/**
 * @author by akurutin@gmail.com on 04.04.2017.
 */
public enum FileExtensionEnum {
  CSV() {
    @Override
    public String toString() {
      return "csv";
    }
  },
  XLS() {
    @Override
    public String toString() {
      return "xls";
    }
  },
  PNG() {
    @Override
    public String toString() {
      return "png";
    }
  },
  JPG() {
    @Override
    public String toString() {
      return "jpg";
    }
  };
}
