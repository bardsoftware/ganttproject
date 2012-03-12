/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject Team

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
package org.ganttproject.impex.htmlpdf.fonts;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import net.sourceforge.ganttproject.GPLogger;

/**
 * @author bard
 */
public class FontMetricsStorage {
  public URI getFontMetricsURI(TTFFileExt ttfFile) {
    URI result = null;
    String fontName = ttfFile.getFile().getName();
    String resourceName = "font-metrics/" + fontName + ".xml";
    URL resourceUrl = getClass().getClassLoader().getResource(resourceName);

    try {
      result = resourceUrl == null ? null : new URI(URLEncoder.encode(resourceUrl.toString(), "utf-8"));
    } catch (URISyntaxException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
    } catch (UnsupportedEncodingException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
    }
    return result;
  }
}
