package net.sourceforge.ganttproject.update;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.client.RssParser;
import net.sourceforge.ganttproject.client.RssUpdate;

public class UpdateParserTest extends TestCase {

  private static final String CONTENT = "3.0.0\n" +
          "http://www.ganttproject.biz/my/feed\n" +
          "GanttProject 3.0.0 is now available.&amp;nbsp;&lt;a href=&quot;https://www.ganttproject.biz/download/upgrade&quot;&gt;Download the update&lt;/a&gt;";


  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testUpdateParser_Sussessful() {
    RssParser parser = new RssParser();
    RssUpdate update = parser.parseUpdate(CONTENT);

    assertEquals("3.0.0",update.getVersion());
    assertEquals("http://www.ganttproject.biz/my/feed", update.getUrl());
    assertEquals("GanttProject 3.0.0 is now available.&amp;nbsp;&lt;a href=&quot;https://www.ganttproject.biz/download/upgrade&quot;&gt;Download the update&lt;/a&gt;", update.getDescription());
  }

  public void testUpdateParser_Empty() {
    RssParser parser = new RssParser();
    RssUpdate update = parser.parseUpdate("");

    assertNull(update);
   }
}
