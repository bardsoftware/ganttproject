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
package net.sourceforge.ganttproject.search;

import java.util.List;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;

/**
 * Interface of a pluggable search service. Given a search query, a search
 * service should be able to search for results and return {@link SearchResult}
 * objects which are shown in the search UI. When user selects some search
 * result, a service should be able to show the result somehow (e.g. make it
 * visible and switch keyboard focus to the result).
 * 
 * @author dbarashev (Dmitry Barashev)
 * 
 * @param <SR>
 *          search result object type
 * @param <SO>
 *          target search object type
 */
public interface SearchService<SR extends SearchResult<SO>, SO> {
  String EXTENSION_POINT_ID = "net.sourceforge.ganttproject.search";

  void init(IGanttProject project, UIFacade uiFacade);

  List<SR> search(String query);

  void select(List<SR> list);
}
