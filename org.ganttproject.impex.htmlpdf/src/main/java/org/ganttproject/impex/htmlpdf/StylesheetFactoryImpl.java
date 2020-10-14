/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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
package org.ganttproject.impex.htmlpdf;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.GPLogger;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

public abstract class StylesheetFactoryImpl {
  public List<Stylesheet> createStylesheets(Class<? extends Stylesheet> stylesheetInterface) {
    IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
    IConfigurationElement[] configElements = extensionRegistry.getConfigurationElementsFor(stylesheetInterface.getName());
    List<Stylesheet> result = new ArrayList<Stylesheet>();
    for (int i = 0; i < configElements.length; i++) {
      try {
        // Object nextExtension =
        // configElements[i].createExecutableExtension("class");
        // assert nextExtension!=null && nextExtension instanceof HTMLStylesheet
        // :
        // "Extension="+nextExtension+" is expected to be instance of HTMLStylesheet";
        String localizedName = configElements[i].getAttribute("name");
        String pluginRelativeUrl = configElements[i].getAttribute("url");
        String namespace = configElements[i].getDeclaringExtension().getNamespaceIdentifier();
        URL stylesheetUrl = Platform.getBundle(namespace).getResource(pluginRelativeUrl);
        assert stylesheetUrl != null : "Failed to resolve url=" + pluginRelativeUrl;
        URL resolvedUrl = Platform.resolve(stylesheetUrl);
        assert resolvedUrl != null : "Failed to resolve URL=" + stylesheetUrl;
        result.add(newStylesheet(resolvedUrl, localizedName));
      } catch (Exception e) {
        if (!GPLogger.log(e)) {
          e.printStackTrace(System.err);
        }
      }
    }
    return result;
  }

  protected abstract Stylesheet newStylesheet(URL url, String localizedName);
}
