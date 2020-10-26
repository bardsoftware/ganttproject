/*
Copyright 2012 GanttProject Team

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
package net.sourceforge.ganttproject.io;

import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.ListOption;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.sax.TransformerHandler;
import java.util.Arrays;
import java.util.Map;

/**
 * Saves GPOption instances to XML as &lt;option&gt; tags.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class OptionSaver extends SaverBase {
  public void saveOptionList(TransformerHandler handler, GPOption<?>... options) throws SAXException {
    saveOptionList(handler, Arrays.asList(options));
  }

  public void saveOptionList(TransformerHandler handler, Iterable<GPOption<?>> options) throws SAXException {
    saveOptionMap(Maps.uniqueIndex(options, new Function<GPOption<?>, String>() {
      @Override
      public String apply(GPOption<?> value) {
        return value.getID();
      }
    }).entrySet(), handler);
  }

  public void saveOptionMap(Iterable<Map.Entry<String, GPOption<?>>> options, TransformerHandler handler) throws SAXException {
    for (Map.Entry<String, GPOption<?>> entry : options) {
      saveOption(entry.getKey(), entry.getValue(), handler);
    }
  }

  public void saveOption(String id, GPOption<?> option, TransformerHandler handler) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    if (option.getPersistentValue() != null) {
      addAttribute("id", id, attrs);
      if (option instanceof ListOption) {
        cdataElement("option", option.getPersistentValue(), attrs, handler);
      } else {
        addAttribute("value", option.getPersistentValue(), attrs);
        emptyElement("option", attrs, handler);
      }
    }
  }
}
