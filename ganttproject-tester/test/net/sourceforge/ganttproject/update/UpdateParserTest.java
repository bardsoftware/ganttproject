/*
Copyright 2018 Oleksii Lapinskyi, BarD Software s.r.o

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
package net.sourceforge.ganttproject.update;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.client.RssFeed;
import net.sourceforge.ganttproject.client.RssParser;
import net.sourceforge.ganttproject.client.RssUpdate;

import java.io.InputStream;
import java.util.List;

public class UpdateParserTest extends TestCase {

  private static final String CONTENT = "3.0.0\n" +
      "http://www.ganttproject.biz/my/feed\n" +
      "GanttProject 3.0.0 is now available.&amp;nbsp;&lt;a href=&quot;https://www.ganttproject.biz/download/upgrade&quot;&gt;Download the update&lt;/a&gt;";

  public void testUpdateParser_Sussessful() {
    RssParser parser = new RssParser("2.8.6", "2262");
    RssUpdate update = parser.parseUpdate(CONTENT);

    assertEquals("3.0.0", update.getVersion());
    assertEquals("http://www.ganttproject.biz/my/feed", update.getUrl());
    assertEquals("GanttProject 3.0.0 is now available.&amp;nbsp;&lt;a href=&quot;https://www.ganttproject.biz/download/upgrade&quot;&gt;Download the update&lt;/a&gt;", update.getDescription());
  }

  public void testUpdateParser_Empty() {
    RssParser parser = new RssParser("2.8.6", "2262");
    RssUpdate update = parser.parseUpdate("");

    assertNull(update);
  }

  public void testParseRss_Successful() {
    InputStream updateRss = UpdateParserTest.class.getResourceAsStream("/update.rss");

    RssParser parser = new RssParser("2.8.6", "2262");
    RssFeed feed = parser.parse(updateRss, null);

    assertNotNull(feed);
    List<RssFeed.Item> items = feed.getItems();
    assertEquals(2, items.size());

    assertFalse(items.get(1).isUpdate);

    RssFeed.Item updateItem = items.get(0);
    assertTrue(updateItem.isUpdate);
    assertNotNull(updateItem.body);

    RssUpdate update = parser.parseUpdate(updateItem.body);
    assertEquals("3.0.0", update.getVersion());
    assertEquals("https://www.dropbox.com/s/exetyj5pk0na3ze/update-2.8.6.zip?dl=1", update.getUrl());
    assertEquals("<b>GanttProject 2.8.6</b> is now available.&nbsp;<a href=\"https://www.ganttproject.biz/download/upgrade\">Download the update</a>\n", update.getDescription());
  }

}
