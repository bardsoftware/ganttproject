/*
Copyright 2014 BarD Software s.r.o

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
package net.sourceforge.ganttproject.parser;

import org.xml.sax.Attributes;

import com.google.common.base.Objects;

import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.ListOption;

/**
 * Tag handler which writes parsed value to GPOption instance.
 *
 * This handler reads "id" and "value" attributes of <option> tag. If id
 * equals to the id of the passed option, handler asks option to load its persistent
 * value from "value" attribute, unless option is a ListOption. In the latter case
 * its value is read from CDATA between the opening and closing tags
 *
 * @author dbarashev (Dmitry Barashev)
 *
 * @param <T> option class
 */
public class OptionTagHandler<T extends GPOption<?>> extends AbstractTagHandler {
  private T myOption;

  public OptionTagHandler(T option) {
    super("option", option instanceof ListOption);
    myOption = option;
  }

  @Override
  protected boolean onStartElement(Attributes attrs) {
    if (!Objects.equal(attrs.getValue("id"), myOption.getID())) {
      return false;
    }
    if (!hasCdata()) {
      myOption.loadPersistentValue(attrs.getValue("value"));
    }
    return super.onStartElement(attrs);
  }

  @Override
  public void onEndElement() {
    if (hasCdata()) {
      myOption.loadPersistentValue(getCdata());
      clearCdata();
    }
  }
}
