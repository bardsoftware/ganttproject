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

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;

import org.ganttproject.impex.htmlpdf.itext.FontSubstitutionModel;
import org.ganttproject.impex.htmlpdf.itext.FontSubstitutionModel.FontSubstitution;
import org.ganttproject.impex.htmlpdf.itext.ITextEngine;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.FontMapper;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.collect.Pair;

/**
 * This class collects True Type fonts from .ttf files in the registered directories
 * and provides mappings of font family names to plain AWT fonts and iText fonts.
 * @author dbarashev
 */
public class TTFontCache {
    private Map<String, Supplier<Font>> myMap_Family_RegularFont = new TreeMap<String,Supplier<Font>>();
    //private Map<String, String> myMap_Family_Filename = new HashMap<String, String>();
    private final Map<Pair<Integer, Float>, com.itextpdf.text.Font> myFontCache =
            new HashMap<Pair<Integer,Float>, com.itextpdf.text.Font>();
    private Map<String, Function<String, BaseFont>> myMap_Family_ItextFont = new HashMap<String, Function<String, BaseFont>>();
    private Properties myProperties;

    public void registerDirectory(String path, boolean recursive) {
        GPLogger.log("reading directory="+path);
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            registerFonts(dir);
        } else {
            GPLogger.log("directory "+path+" is not readable");
        }
    }

    public List<String> getRegisteredFamilies() {
        return new ArrayList<String>(myMap_Family_RegularFont.keySet());
    }

    public Font getAwtFont(String family) {
        Supplier<Font> supplier = myMap_Family_RegularFont.get(family);
        return supplier == null ? null : supplier.get();
    }

    private void registerFonts(File dir) {
        boolean runningUnderJava6;
        try {
            Font.class.getMethod("createFont", new Class[] {Integer.TYPE, File.class});
            runningUnderJava6 = true;
        } catch (SecurityException e) {
            runningUnderJava6 = false;
        } catch (NoSuchMethodException e) {
            runningUnderJava6 = false;
        }
        final File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                registerFonts(f);
                continue;
            }
            if (!f.getName().toLowerCase().trim().endsWith(".ttf")) {
                continue;
            }
            try {
                registerFontFile(f, runningUnderJava6);
            } catch (Throwable e) {
                GPLogger.getLogger(ITextEngine.class).log(
                    Level.INFO, "Failed to register font from " + f.getAbsolutePath(), e);
            }
       }
    }

    private static Font createAwtFont(File fontFile, boolean runningUnderJava6) throws IOException, FontFormatException {
        return runningUnderJava6 ?
                Font.createFont(Font.TRUETYPE_FONT, fontFile) :
                Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(fontFile));
    }

    private void registerFontFile(final File fontFile, final boolean runningUnderJava6) throws FontFormatException, IOException {
        //FontFactory.register(fontFile.getAbsolutePath());
        Font awtFont = createAwtFont(fontFile, runningUnderJava6);

        final String family = awtFont.getFamily().toLowerCase();
        if (myMap_Family_RegularFont.containsKey(family)) {
            return;
        }

//        // We will put a font to the mapping only if it is a plain font.
//        final com.itextpdf.text.Font itextFont = FontFactory.getFont(family, 12f, com.itextpdf.text.Font.NORMAL);
//        if (itextFont == null || itextFont.getBaseFont() == null) {
//            return;
//        }

        GPLogger.log("registering font: " + family);
        myMap_Family_RegularFont.put(family, Suppliers.<Font>memoize(new Supplier<Font>() {
            @Override
            public Font get() {
                try {
                    return createAwtFont(fontFile, runningUnderJava6);
                } catch (IOException e) {
                    GPLogger.log(e);
                } catch (FontFormatException e) {
                    GPLogger.log(e);
                }
                return null;
            }
        }));
        //myMap_Family_Filename.put(family, fontFile.getAbsolutePath());
        try {
            BaseFont.createFont(
                    fontFile.getAbsolutePath(), GanttLanguage.getInstance().getCharSet(), BaseFont.EMBEDDED);
            myMap_Family_ItextFont.put(family, new Function<String, BaseFont>() {
                @Override
                public BaseFont apply(String charset) {
                    try {
                        return BaseFont.createFont(
                                fontFile.getAbsolutePath(), charset, BaseFont.EMBEDDED);
                    } catch (DocumentException e) {
                        GPLogger.log(e);
                    } catch (IOException e) {
                        GPLogger.log(e);
                    }
                    return null;
                }
            });
        } catch (DocumentException e) {
            if (e.getMessage().indexOf("cannot be embedded") > 0) {
                GPLogger.logToLogger("Font " + family + " from " + fontFile.getAbsolutePath() + " skipped due to licensing restrictions");
            } else {
                e.printStackTrace();
            }
        }
    }

    public com.itextpdf.text.Font getFont(String family, String charset, int style, float size) {
        Pair<Integer, Float> key = Pair.create(style, size);
        com.itextpdf.text.Font result = myFontCache.get(key);
        if (result == null) {
            BaseFont bf = myMap_Family_ItextFont.get(family).apply(charset);
            if (bf != null) {
                result = new com.itextpdf.text.Font(bf, size);
                myFontCache.put(key, result);
            }
        }
        return result;

    }

    public FontMapper getFontMapper(final FontSubstitutionModel substitutions, final String charset) {
        return new FontMapper() {
            @Override
            public BaseFont awtToPdf(Font awtFont) {
                String family = awtFont.getFamily().toLowerCase();
                if (myProperties.containsKey("font." + family)) {
                    family = String.valueOf(myProperties.get("font." + family));
                }
                FontSubstitution substitution = substitutions.getSubstitution(family);
                if (substitution != null) {
                    family = substitution.getSubstitutionFamily();
                }
                Function<String, BaseFont> f = myMap_Family_ItextFont.get(family);
                return f == null ? null : f.apply(charset);
            }

            @Override
            public Font pdfToAwt(BaseFont itextFont, int size) {
                return null;
            }

        };
    }

    public void setProperties(Properties properties) {
        myProperties = properties;
    }
}
