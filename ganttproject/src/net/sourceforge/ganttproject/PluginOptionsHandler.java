/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject;

/*
 GanttProject is an opensource project management tool.
 Copyright (C) 2009 Dmitry Barashev

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
import org.osgi.service.prefs.Preferences;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class PluginOptionsHandler extends DefaultHandler {
  private Preferences myCurrentNode;

  PluginOptionsHandler(PluginPreferencesImpl pluginPreferencesRootNode) {
    myCurrentNode = pluginPreferencesRootNode;
  }

  @Override
  public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
    if ("option".equals(name)) {
      myCurrentNode.put(attributes.getValue("name"), attributes.getValue("value"));
      return;
    }
    myCurrentNode = ((PluginPreferencesImpl) myCurrentNode).createChild(name);
  }

  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    if (name.equals(myCurrentNode.name())) {
      myCurrentNode = myCurrentNode.parent();
    }
  }
}
