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
package net.sourceforge.ganttproject;

/**
 * Class to store the project information
 * 
 * @author athomas
 */
public class PrjInfos {
  /** The name of the project */
  private String _sProjectName;

  /** A short description of it */
  private String _sDescription;

  /** The name of the organization */
  private String _sOrganization;

  /** Web link for the project or for the company */
  private String _sWebLink;

  public PrjInfos() {
    this._sProjectName = "Untitled Gantt Project";
    this._sDescription = "";
    this._sOrganization = "";
    this._sWebLink = "http://";
  }

  public PrjInfos(String sProjectName, String sDescription, String sOrganization, String sWebLink) {
    this._sProjectName = sProjectName;
    this._sDescription = sDescription;
    this._sOrganization = sOrganization;
    this._sWebLink = sWebLink;
  }

  /** @return the name of the project. */
  public String getName() {
    return _sProjectName;
  }

  /** sets the name of the project. */
  public void setName(String projectName) {
    _sProjectName = projectName;
  }

  /** @return the description of the project. */
  public String getDescription() {
    return _sDescription;
  }

  /** sets the description of the project. */
  public void setDescription(String description) {
    _sDescription = description;
  }

  /** @return the organization of the project. */
  public String getOrganization() {
    return _sOrganization;
  }

  /** sets the organization of the project. */
  public void setOrganization(String organization) {
    _sOrganization = organization;
  }

  /** @return the web link for the project or for the company. */
  public String getWebLink() {
    return _sWebLink;
  }

  /** sets the web link for the project or for the company. */
  public void setWebLink(String webLink) {
    _sWebLink = webLink;
  }
}
