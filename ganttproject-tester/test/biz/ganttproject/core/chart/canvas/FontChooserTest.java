package biz.ganttproject.core.chart.canvas;

import java.awt.Font;
import java.util.Properties;

import junit.framework.TestCase;

import com.google.common.base.Suppliers;

public class FontChooserTest extends TestCase {

  private Font myBaseFont;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myBaseFont = Font.decode("Dialog 10");
  }

  public void testNoFontStyle() {
    Properties p = new Properties();
    FontChooser chooser = new FontChooser(p, null, Suppliers.ofInstance(myBaseFont));
    Font font = chooser.getFont("nosuchstyle");
    assertEquals(10, font.getSize());
    assertEquals("Dialog", font.getFamily());
  }

  public void testBaseFontIncrement() {
    Properties p = new Properties();
    p.put("bigger.font", "+1");
    p.put("smaller.font", "-1");
    FontChooser chooser = new FontChooser(p, null, Suppliers.ofInstance(myBaseFont));

    {
      Font font = chooser.getFont("bigger");
      assertEquals(11, font.getSize());
      assertEquals("Dialog", font.getFamily());
    }
    {
      Font font = chooser.getFont("smaller");
      assertEquals(9, font.getSize());
      assertEquals("Dialog", font.getFamily());
    }
  }

  public void testBaseFontAbsoluteValue() {
    Properties p = new Properties();
    p.put("bigger.font", "18");
    FontChooser chooser = new FontChooser(p, null, Suppliers.ofInstance(myBaseFont));
    {
      Font font = chooser.getFont("bigger");
      assertEquals(18, font.getSize());
      assertEquals("Dialog", font.getFamily());
    }
  }

  public void testCustomFamilyIncrement() {
    Properties p = new Properties();
    p.put("custom.bigger.font", "Serif +1");
    p.put("custom.smaller.font", "Serif -2");
    p.put("custom.samesize.font", "Serif +0");

    FontChooser chooser = new FontChooser(p, null, Suppliers.ofInstance(myBaseFont));
    {
      Font font = chooser.getFont("custom.bigger");
      assertEquals(11, font.getSize());
      assertEquals("Serif", font.getFamily());
    }
    {
      Font font = chooser.getFont("custom.smaller");
      assertEquals(8, font.getSize());
      assertEquals("Serif", font.getFamily());
    }
    {
      Font font = chooser.getFont("custom.samesize");
      assertEquals(10, font.getSize());
      assertEquals("Serif", font.getFamily());
    }
  }

  public void testCustomFamilyAbsolute() {
    Properties p = new Properties();
    p.put("custom.bigger.font", "Serif 18");
    p.put("custom.smaller.font", "Serif 8");
    FontChooser chooser = new FontChooser(p, null, Suppliers.ofInstance(myBaseFont));
    {
      Font font = chooser.getFont("custom.bigger");
      assertEquals(18, font.getSize());
      assertEquals("Serif", font.getFamily());
    }
    {
      Font font = chooser.getFont("custom.smaller");
      assertEquals(8, font.getSize());
      assertEquals("Serif", font.getFamily());
    }
  }

  public void testCustomFontSpec() {
    Properties p = new Properties();
    p.put("custom.bigger.font", "Serif-BOLD-16");
    p.put("custom.smaller.font", "Serif-ITALIC-8");
    FontChooser chooser = new FontChooser(p, null, Suppliers.ofInstance(myBaseFont));
    {
      Font font = chooser.getFont("custom.bigger");
      assertEquals(16, font.getSize());
      assertEquals("Serif", font.getFamily());
      assertEquals(Font.BOLD, font.getStyle());
    }
    {
      Font font = chooser.getFont("custom.smaller");
      assertEquals(8, font.getSize());
      assertEquals("Serif", font.getFamily());
      assertEquals(Font.ITALIC, font.getStyle());
    }
  }
}
