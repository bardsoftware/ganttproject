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
package org.ganttproject.impex.htmlpdf.fonts;

import biz.ganttproject.app.FontManager;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.itextpdf.awt.FontMapper;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.ganttproject.impex.htmlpdf.itext.FontSubstitutionModel;
import org.ganttproject.impex.htmlpdf.itext.FontSubstitutionModel.FontSubstitution;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This class collects True Type fonts from .ttf files in the registered
 * directories and provides mappings of font family names to plain AWT fonts and
 * iText fonts.
 *
 * @author dbarashev
 */
public class TTFontCache {
  private static final org.slf4j.Logger ourLogger = GPLogger.create("Export.Pdf.Fonts").delegate();
  private final Map<String, AwtFontSupplier> myMap_Family_RegularFont = new TreeMap<>();
  private final Map<FontKey, com.itextpdf.text.Font> myFontCache = new HashMap<>();
  private final Map<String, Function<String, BaseFont>> myMap_Family_ItextFont = new HashMap<>();
  private Properties myProperties;
  private Function<String, BaseFont> myFallbackFont;

  public TTFontCache() {
    try {
      myFallbackFont = createFontSupplier(FontManager.INSTANCE.getFallbackFontFile(), true);
      registerFontFile(FontManager.INSTANCE.getFallbackFontFile());
    } catch (Exception e) {
      ourLogger.error("Failed to create fallback font", e);
    }
  }

  public void registerDirectory(String path) {
    File dir = new File(path);
    if (dir.exists() && dir.isDirectory()) {
      registerFonts(dir);
    } else {
      ourLogger.info("Skipping directory {}", path);
    }
  }

  public List<String> getRegisteredFamilies() {
    return new ArrayList<>(myMap_Family_RegularFont.keySet());
  }

  public Font getAwtFont(String family) {
    Supplier<Font> supplier = myMap_Family_RegularFont.get(family);
    return supplier == null ? null : supplier.get();
  }

  private void registerFonts(File dir) {
    final File[] files = dir.listFiles(pathname -> {
      var name = pathname.getName();
      return pathname.isDirectory() || name.toLowerCase().endsWith(".ttf") || name.toLowerCase().endsWith(".ttc");
    });
    if (files == null) {
      return;
    }
    ourLogger.debug("registerFonts: dir={} |files|={}", dir.getAbsolutePath(), files.length);
    for (File f : files) {
      if (!f.canRead()) {
        ourLogger.warn("Can't read the font file={}", f.getAbsolutePath());
        continue;
      }
      if (f.isDirectory()) {
        registerFonts(f);
        continue;
      }
      String filename = f.getName().toLowerCase().trim();
      if (!filename.endsWith(".ttf") && !filename.endsWith(".ttc")) {
        ourLogger.warn("Skipping file={} because of its extension", f.getAbsolutePath());
        continue;
      }
      try {
        registerFontFile(f);
      } catch (Throwable e) {
        ourLogger.error("Failed to register font={}", f.getAbsolutePath(), e);
      }
    }
  }

  private static Font createAwtFont(File fontFile) throws IOException, FontFormatException {
    try (FileInputStream istream = new FileInputStream(fontFile)) {
      return Font.createFont(Font.TRUETYPE_FONT, istream);
    }
  }

  private static class AwtFontSupplier implements Supplier<Font> {
    private final List<File> myFiles = Lists.newArrayList();
    private final String family;
    private Font myFont;

    private AwtFontSupplier(String family) {
      this.family = family;
    }
    void addFile(File f) {
      myFiles.add(f);
    }

    @Override
    public Font get() {
      if (myFont == null) {
        myFont = createFont();
        ourLogger.debug("Created AWT font={} for family={}", myFont==null ? "null" : myFont.toString(), family);
      }
      return myFont;
    }

    private Font createFont() {
      Font result = null;
      for (File f : myFiles) {
        Font font = createFont(f);
        if (result == null || result.getStyle() > font.getStyle()) {
          result = font;
        }
      }
      return result;
    }

    private Font createFont(File fontFile) {
      try {
        return createAwtFont(fontFile);
      } catch (IOException | FontFormatException e) {
        ourLogger.error("Failed when creating AWT font from file {}", fontFile, e);
      }
      return null;
    }
  }

  private void registerFontFile(final File fontFile) throws FontFormatException,
      IOException {
    Font awtFont = createAwtFont(fontFile);

    final String family = awtFont.getFontName().toLowerCase();
    ourLogger.debug("Registering font: file={} family={}", fontFile.getName(), family);
    AwtFontSupplier awtSupplier = myMap_Family_RegularFont.get(family);
    if (awtSupplier == null) {
      awtSupplier = new AwtFontSupplier(family);
      myMap_Family_RegularFont.put(family, awtSupplier);
    }
    awtSupplier.addFile(fontFile);
    try {
      var fontSupplier = createFontSupplier(fontFile, BaseFont.EMBEDDED);
      if (fontSupplier != null) {
        myMap_Family_ItextFont.put(family, fontSupplier);
      } else {
        ourLogger.error("Failed to create PDF font supplier for the fallback font={} file={}",
            family, fontFile.getAbsolutePath());
      }
    } catch (DocumentException e) {
      ourLogger.error("Failed to create PDF font supplier for the fallback font={} file={}", family, fontFile.getAbsolutePath(), e);
    }
  }

  private Function<String, BaseFont> createFontSupplier(final File fontFile, final boolean isEmbedded)
      throws DocumentException {
    Function<String, BaseFont> result = charset -> {
      try {
        if (fontFile.getName().toLowerCase().endsWith(".ttc")) {
          return BaseFont.createFont(fontFile.getAbsolutePath() + ",0", charset, isEmbedded);
        } else {
          return BaseFont.createFont(fontFile.getAbsolutePath(), charset, isEmbedded);
        }
      } catch (DocumentException | IOException e) {
        ourLogger.error("Failure when creating PDF font from file={} for charset={}",
          fontFile.getAbsolutePath(), charset, e);
      }
      return null;
    };
    try {
      // This is just a test if we can create the PDF font or not. In case of success we return the supplier
      // which returns the font, otherwise we return null and clear the caches to reduce memory footprint.
      BaseFont baseFont = result.apply(GanttLanguage.getInstance().getCharSet());
      return baseFont == null ? null : result;
    } catch (Exception e) {
      ourLogger.error("Failure when creating PDF font from file={} for charset={}",
          fontFile.getName(), GanttLanguage.getInstance().getCharSet(), e);
      return null;
    } finally {
      BaseFontPublicMorozov.clearCache();
    }
  }

  private static class FontKey {
    private final String family;
    private final int style;
    private final float size;
    private final String charset;

    FontKey(String family, String charset, int style, float size) {
      this.family = family;
      this.charset = charset;
      this.style = style;
      this.size = size;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((charset == null) ? 0 : charset.hashCode());
      result = prime * result + ((family == null) ? 0 : family.hashCode());
      result = prime * result + Float.floatToIntBits(size);
      result = prime * result + style;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      FontKey other = (FontKey) obj;
      if (charset == null) {
        if (other.charset != null)
          return false;
      } else if (!charset.equals(other.charset))
        return false;
      if (family == null) {
        if (other.family != null)
          return false;
      } else if (!family.equals(other.family))
        return false;
      if (Float.floatToIntBits(size) != Float.floatToIntBits(other.size))
        return false;
      if (style != other.style)
        return false;
      return true;
    }
  }

  public com.itextpdf.text.Font getFont(String family, String charset, int style, float size) {
    FontKey key = new FontKey(family, charset, style, size);
    com.itextpdf.text.Font result = myFontCache.get(key);
    if (result == null) {
      Function<String, BaseFont> f = myMap_Family_ItextFont.get(family);
      BaseFont bf = f == null ? getFallbackFont(charset) : f.apply(charset);
      if (bf != null) {
        result = new com.itextpdf.text.Font(bf, size, style);
        myFontCache.put(key, result);
      } else {
        ourLogger.error("Can't find a PDF font corresponding to AWT family={} charset={}", family, charset);
      }
    }
    return result;

  }

  public FontMapper getFontMapper(final FontSubstitutionModel substitutions, final String charset) {
    return new FontMapper() {
      private final Map<Font, BaseFont> myFontCache = new HashMap<>();

      @Override
      public BaseFont awtToPdf(Font awtFont) {
        ourLogger.debug("Searching for BaseFont for awtFont={} charset={} cache={}", awtFont, charset, myFontCache);
        if (myFontCache.containsKey(awtFont)) {
          ourLogger.debug("Found in cache.");
          return myFontCache.get(awtFont);
        }

        String family = awtFont.getFamily().toLowerCase();
        Function<String, BaseFont> f = myMap_Family_ItextFont.get(family);
        ourLogger.debug("Searching for supplier: family={} size={}", family, awtFont.getSize());
        if (f != null) {
          BaseFont result = f.apply(charset);
          if (result == null) {
            ourLogger.warn("... failed to find the base font for family={} charset={}", family, charset);
          } else {
            ourLogger.debug("... created BaseFont: {}", getInfo(result));
            myFontCache.put(awtFont, result);
          }
          return result;
        }

        family = family.replace(' ', '_');
        if (myProperties.containsKey("font." + family)) {
          family = String.valueOf(myProperties.get("font." + family));
        }
        ourLogger.debug("Searching for substitution. Family={}", family);
        FontSubstitution substitution = substitutions.getSubstitution(family);
        if (substitution != null) {
          family = substitution.getSubstitutionFamily();
        }
        f = myMap_Family_ItextFont.get(family);
        ourLogger.debug("substitution family={} supplier={}", family, f);
        if (f != null) {
          BaseFont result = f.apply(charset);
          ourLogger.debug("created base font={}", result);
          myFontCache.put(awtFont, result);
          return result;
        }
        BaseFont result = getFallbackFont(charset);
        ourLogger.debug("so, trying fallback font={}", result);
        if (result == null) {
          ourLogger.error("Can't find a PDF font corresponding to AWT font with family={}. " +
              "Also tried substitution family={} and fallback font. Charset={}", awtFont.getFamily(), family, charset);
        }
        return result;
      }

      @Override
      public Font pdfToAwt(BaseFont itextFont, int size) {
        return null;
      }

    };
  }

  protected BaseFont getFallbackFont(String charset) {
    return myFallbackFont.apply(charset);
  }

  public void setProperties(Properties properties) {
    myProperties = properties;
  }

  // BaseFont.fontCache is a static map which caches font objects. Since we scan all
  // fonts in this code, we may cache a few hundreds of objects, and retained size of each object
  // can be up to a few megabytes. Here we use so-called "Public Morozov" anti-pattern
  // which discloses protected fields of its parent class
  // See description of this pattern in English here:
  // http://jamesdolan.blogspot.com/2011/05/pavlik-morozov-anti-pattern.html
  private static abstract class BaseFontPublicMorozov extends BaseFont {
    static void clearCache() {
      BaseFont.fontCache.clear();
    }
  }

  private static String getInfo(BaseFont font) {
    return Arrays.stream(font.getFullFontName())
      .filter(Objects::nonNull)
      .map(record -> String.join(", ", record))
      .collect(Collectors.toList()).toString();
  }
}
