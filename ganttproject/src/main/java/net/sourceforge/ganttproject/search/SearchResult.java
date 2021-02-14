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
  public static SearchResult EMPTY = new SearchResult(-1, "", "", "", "", "", null, null);
  private final int myId;
  private final String mySecondaryLabel;
  private final String myTypeOfResult;
  private final String mySecondaryText;
  private final String myLabel; //Top line
  private final String myQueryMatch;
  private final SearchService<?, SearchObjectType> mySearchService;
  private final SearchObjectType mySearchObject;

  SearchResult(
      int id,
      String typeOfResult,
      String label,
      String queryMatch,
      String secondaryLabel,
      String secondaryText,
      SearchObjectType searchObject,
      SearchService<?, SearchObjectType> service
  ) {
    myId = id;
    myTypeOfResult = typeOfResult;
    myLabel = label;
    myQueryMatch = queryMatch;
    mySecondaryText = secondaryText;
    mySecondaryLabel = secondaryLabel;
    mySearchService = service;
    mySearchObject = searchObject;
  }

  int getId() { return myId; }

  public String getTypeOfResult() { return myTypeOfResult; }

  public String getSecondaryText() { return mySecondaryText; }

  public String getQueryMatch() { return myQueryMatch; }

  public String getLabel() {
    return myLabel;
  }

  public String getSecondaryLabel() { return mySecondaryLabel; }

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

