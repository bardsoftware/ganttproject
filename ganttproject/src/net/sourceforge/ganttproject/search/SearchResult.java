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

public class SearchResult<SearchObjectType> {
  private final String myOrigin;
  private final String mySnippet;
  private final String myLabel;
  private final SearchService<?, SearchObjectType> mySearchService;
  private final SearchObjectType mySearchObject;

  public SearchResult(String label, String snippet, String origin, SearchObjectType searchObject,
      SearchService<?, SearchObjectType> service) {
    myLabel = label;
    mySnippet = snippet;
    myOrigin = origin;
    mySearchService = service;
    mySearchObject = searchObject;
  }

  public String getLabel() {
    return myLabel;
  }

  public String getSnippet() {
    return mySnippet;
  }

  public String getOrigin() {
    return myOrigin;
  }

  public SearchObjectType getObject() {
    return mySearchObject;
  }

  @Override
  public String toString() {
    return myLabel;
  }

  public SearchService<?, SearchObjectType> getSearchService() {
    return mySearchService;
  }
}
