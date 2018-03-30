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
  private final int id;
  private final String myOrigin;
  private final String secondaryLabel;
  private final String typeOfResult;
  private final String secondaryText;
  private final String myLabel; //Top line
  private final String myQueryMatch;
  private final SearchService<?, SearchObjectType> mySearchService;
  private final SearchObjectType mySearchObject;

  public SearchResult(
          int id,
          String typeOfResult,
          String label,
          String myQueryMatch,
          String secondaryLabel,
          String secondaryText,
          String origin,
          SearchObjectType searchObject,
          SearchService<?, SearchObjectType> service
  ) {
    this.id = id;
    this.typeOfResult = typeOfResult;
    myLabel = label;
    this.myQueryMatch = myQueryMatch;
    this.secondaryText = secondaryText;
    this.secondaryLabel = secondaryLabel;
    myOrigin = origin;
    mySearchService = service;
    mySearchObject = searchObject;
  }

  public int getId() { return id; }

  public String getTypeOfResult() { return typeOfResult; }

  public String getSecondaryText() { return secondaryText; }

  public String getMyQueryMatch() { return myQueryMatch; }

  public String getLabel() {
    return myLabel;
  }

  public String getSecondaryLabel() { return secondaryLabel; }

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
