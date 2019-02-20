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
package net.sourceforge.ganttproject.client;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import net.sourceforge.ganttproject.GPVersion;
import net.sourceforge.ganttproject.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Iterator;

public class RssParser {

  private final XPathFactory myXPathFactory = XPathFactory.newInstance();
  private final String myCurrentBuildNumber;
  private final String myCurrentVersionNumber;

  RssParser() {
    this(GPVersion.getCurrentVersionNumber(), GPVersion.getCurrentBuildNumber());
  }

  public RssParser(String currentVersionNumber, String currentBuildNumber) {
    myCurrentVersionNumber = Preconditions.checkNotNull(currentVersionNumber);
    myCurrentBuildNumber = Preconditions.checkNotNull(currentBuildNumber);
  }

  private XPathExpression getXPath(String expression) throws XPathExpressionException {
    XPath xpath = myXPathFactory.newXPath();
    xpath.setNamespaceContext(new NamespaceContext() {
      @Override
      public String getNamespaceURI(String s) {
        if ("atom".equals(s)) {
          return "http://www.w3.org/2005/Atom";
        }
        throw new IllegalArgumentException(s);
      }

      @Override
      public String getPrefix(String s) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Iterator<String> getPrefixes(String s) {
        throw new UnsupportedOperationException();
      }
    });
    return xpath.compile(expression);
  }

  public RssFeed parse(InputStream inputStream, Date lastCheckDate) {
    RssFeed result = new RssFeed();
    try {
      XPathExpression xpath = getXPath("//atom:entry");
      NodeList items = (NodeList) xpath.evaluate(new InputSource(new InputStreamReader(inputStream)),
          XPathConstants.NODESET);
      for (int i = 0; i < items.getLength(); i++) {
        if (isApplicableToVersion(items.item(i), myCurrentVersionNumber, myCurrentBuildNumber)) {
          addItem(result, items.item(i), lastCheckDate);
        }
      }

    } catch (XPathExpressionException e) {
      e.printStackTrace();
    }
    return result;
  }

  public RssUpdate parseUpdate(String content) {
    if (!StringUtils.isEmptyOrNull(content)) {

      String[] parts = content.split("\n", 3);
      if (parts.length == 3) {
        return new RssUpdate(parts[0], parts[1], parts[2]);
      }
    }
    return null;
  }

  private boolean isApplicableToVersion(Node item, String version, String build) throws XPathExpressionException {
    boolean hasRestriction = false;
    NodeList categories = (NodeList) getXPath("atom:category").evaluate(item, XPathConstants.NODESET);
    for (int i = 0; i < categories.getLength(); i++) {
      Element elCategory = (Element) categories.item(i);
      String category = elCategory.getAttribute("term");
      if (!Strings.isNullOrEmpty(category)) {
        if (category.startsWith("__version")) {
          hasRestriction = true;
          if (compareCategory(category, "version", version)) {
            return true;
          }
        }
        if (category.startsWith("__build")) {
          hasRestriction = true;
          if (compareCategory(category, "build", build)) {
            return true;
          }
        }
      }
    }
    return !hasRestriction;
  }

  private boolean compareCategory(String category, String type, String value) {
    if (category.startsWith("__" + type + "_lt_")) {
      String valueRequired = category.substring(("__" + type + "_lt_").length());
      return value.compareTo(valueRequired) < 0;
    } else if (category.startsWith("__" + type + "_gt_")) {
      String valueRequired = category.substring(("__" + type + "_gt_").length());
      return value.compareTo(valueRequired) > 0;
    } else if (category.startsWith("__" + type + "_eq_")) {
      String valueRequired = category.substring(("__" + type + "_eq_").length());
      return value.equals(valueRequired);
    }
    return false;
  }

  private void addItem(RssFeed result, Node item, Date lastCheckDate) throws XPathExpressionException {
    if (lastCheckDate != null) {
      String updateString = getXPath("atom:updated/text()").evaluate(item);
      try {
        Date updateDate = DateParser.parse(updateString);
        if (updateDate.before(lastCheckDate)) {
          return;
        }
      } catch (InvalidDateException e) {
        e.printStackTrace();
      }
    }
    String title = getXPath("atom:title/text()").evaluate(item);
    String body = getXPath("atom:content/text()").evaluate(item);
    boolean isUpdate = isUpdateItem(item);
    result.addItem(title, body, isUpdate);
  }

  private boolean isUpdateItem(Node item) throws XPathExpressionException {
    NodeList categories = (NodeList) getXPath("atom:category").evaluate(item, XPathConstants.NODESET);
    for (int i = 0; i < categories.getLength(); i++) {
      Element elCategory = (Element) categories.item(i);
      String category = elCategory.getAttribute("term");
      if (!Strings.isNullOrEmpty(category)) {
        if (category.equals("update")) {
          return true;
        }
      }
    }
    return false;
  }
}
