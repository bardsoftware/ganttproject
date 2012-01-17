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
import java.util.TreeMap;
import java.util.logging.Level;

import org.ganttproject.impex.htmlpdf.itext.ITextEngine;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.BaseFont;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.collect.Pair;

/**
 * This class collects True Type fonts from .ttf files in the registered directories
 * and provides mappings of font family names to plain AWT fonts and iText fonts.
 * @author dbarashev
 */
public class TTFontCache {
    private Map<String,Font> myMap_Family_RegularFont = new TreeMap<String,Font>();
    private Map<String, String> myMap_Family_Filename = new HashMap<String, String>();
    private final Map<Pair<Integer, Float>, com.itextpdf.text.Font> myFontCache =
            new HashMap<Pair<Integer,Float>, com.itextpdf.text.Font>();

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
        return (Font) myMap_Family_RegularFont.get(family);
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

    private void registerFontFile(File fontFile, boolean runningUnderJava6) throws FontFormatException, IOException {
        FontFactory.register(fontFile.getAbsolutePath());
        Font awtFont = runningUnderJava6 ?
            Font.createFont(Font.TRUETYPE_FONT, fontFile) :
            Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(fontFile));

        final String family = awtFont.getFamily().toLowerCase();
        if (myMap_Family_RegularFont.containsKey(family)) {
            return;
        }

        // We will put a font to the mapping only if it is a plain font.
        final com.itextpdf.text.Font itextFont = FontFactory.getFont(family, 12f, com.itextpdf.text.Font.NORMAL);
        if (itextFont == null || itextFont.getBaseFont() == null) {
            return;
        }

        GPLogger.log("registering font: " + family);
        myMap_Family_RegularFont.put(family, awtFont);
        myMap_Family_Filename.put(family, fontFile.getAbsolutePath());
    }

    public com.itextpdf.text.Font getFont(String family, float size) {
        Pair<Integer, Float> key = Pair.create(com.itextpdf.text.Font.NORMAL, size);
        com.itextpdf.text.Font result = myFontCache.get(key);
        if (result == null) {
            //FontFactory.getFont(getFontName(), GanttLanguage.getInstance().getCharSet(), size);
            String filename = myMap_Family_Filename.get(family);
            if (filename != null) {
                try {
                    BaseFont bf = BaseFont.createFont(filename, GanttLanguage.getInstance().getCharSet(), BaseFont.EMBEDDED);
                    result = new com.itextpdf.text.Font(bf, size);
                    myFontCache.put(key, result);
                } catch (DocumentException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;

    }
}
