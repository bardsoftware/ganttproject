/*
GanttProject is an opensource project management tool.
Copyright (C) 2009 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.ganttproject.GPLogger;

import com.lowagie.text.FontFactory;

/**
 * This class collects True Type fonts from .ttf files in the registered directories
 * and provides mappings of font family names to plain AWT fonts and iText fonts. 
 * @author dbarashev
 */
public class TTFontCache {
    private Map<String,Font> myMap_Family_RegularFont = new TreeMap<String,Font>();

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
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                registerFonts(files[i]);
                continue;
            }
            if (!files[i].getName().toLowerCase().trim().endsWith(".ttf")) {
                continue;
            }
            try {
                registerFontFile(files[i], runningUnderJava6);
            } catch (Exception e) {
                GPLogger.log(e);
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
        final com.lowagie.text.Font itextFont = FontFactory.getFont(family, 12f, com.lowagie.text.Font.NORMAL);                
        if (itextFont == null || itextFont.getBaseFont() == null) {
            return;
        }
                
        GPLogger.log("registering font: " + family);
        myMap_Family_RegularFont.put(family, awtFont);
    }
}
