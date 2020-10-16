/*
Copyright 2013 BarD Software s.r.o

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
import com.google.common.base.Preconditions;

/**
 * Base class for all tag handlers.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public abstract class AbstractTagHandler implements TagHandler {

  private final String myTagName;
  private final StringBuilder myCdataBuffer;
  private boolean myTagStarted;

  protected AbstractTagHandler(String tagName, boolean hasCdata) {
    myTagName = tagName;
    myCdataBuffer = hasCdata ? new StringBuilder() : null;
  }

  protected AbstractTagHandler(String tagName) {
    this(tagName, false);
  }

  @Override
  public boolean hasCdata() {
    return myCdataBuffer != null;
  }

  @Override
  public void appendCdata(String cdata) {
    assert hasCdata() : "It is a bug: this method should not be called for a tag which has no cdata";
    if (myTagStarted) {
      myCdataBuffer.append(cdata);
    }
  }

  protected void setTagStarted(boolean started) {
    myTagStarted = started;
    if (!started && hasCdata()) {
      // we clear accumulated CDATA value when tag which contains CDATA closes
      clearCdata();
    }
  }

  protected boolean isMyTag(String tagName) {
    Preconditions.checkNotNull(myTagName);
    return myTagName.equals(tagName);
  }

  protected boolean isTagStarted() {
    return myTagStarted;
  }

  protected String getCdata() {
    return myCdataBuffer.toString();
  }

  protected void clearCdata() {
    myCdataBuffer.setLength(0);
  }

  @Override
  public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
      throws FileFormatException {
    Preconditions.checkNotNull(myTagName, "If you don't define tag name then please override this method");
    if (Objects.equal(myTagName, qName)) {
      myTagStarted = onStartElement(attrs);
    }
  }

  protected boolean onStartElement(Attributes attrs) {
    return true;
  }

  @Override
  public void endElement(String namespaceURI, String sName, String qName) {
    if (myTagStarted && Objects.equal(myTagName, qName)) {
      myTagStarted = false;
      onEndElement();
    }
  }

  protected void onEndElement() {
  }

}
