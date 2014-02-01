/*
GanttProject is an opensource project management tool.
Copyright (C) 2009-2011 Dmitry Barashev, GanttProject Team

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
package org.ganttproject.impex.htmlpdf.itext;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ganttproject.impex.htmlpdf.fonts.TTFontCache;
import org.osgi.service.prefs.Preferences;

public class FontSubstitutionModel {

  public static class FontSubstitution {
    final TTFontCache myFontCache;
    final String myOriginalFamily;
    private final Preferences myPrefs;

    public FontSubstitution(String family, Preferences prefs, TTFontCache fontCache) {
      myFontCache = fontCache;
      myOriginalFamily = family;
      myPrefs = prefs;
    }

    public boolean isResolved() {
      return getSubstitutionFont() != null;
    }

    public void setSubstitutionFamily(String family) {
      myPrefs.put(myOriginalFamily, family);
    }

    public String getSubstitutionFamily() {
      return myPrefs.get(myOriginalFamily, myOriginalFamily);
    }

    public Font getSubstitutionFont() {
      return myFontCache.getAwtFont(getSubstitutionFamily());
    }
  }

  private final TTFontCache myFontCache;
  private final Map<String, FontSubstitution> mySubstitutions = new LinkedHashMap<String, FontSubstitution>();
  private final ArrayList<FontSubstitution> myIndexedSubstitutions;

  public FontSubstitutionModel(TTFontCache fontCache, ITextStylesheet stylesheet, Preferences prefs) {
    myFontCache = fontCache;
    List<FontSubstitution> unresolvedFonts = new ArrayList<FontSubstitution>();
    List<FontSubstitution> resolvedFonts = new ArrayList<FontSubstitution>();
    for (Iterator<String> families = stylesheet.getFontFamilies().iterator(); families.hasNext();) {
      String nextFamily = families.next();
      FontSubstitution fs = new FontSubstitution(nextFamily, prefs, myFontCache);
      Font awtFont = fs.getSubstitutionFont();
      if (awtFont == null) {
        unresolvedFonts.add(fs);
      } else {
        resolvedFonts.add(fs);
      }
    }
    addSubstitutions(unresolvedFonts);
    addSubstitutions(resolvedFonts);
    myIndexedSubstitutions = new ArrayList<FontSubstitution>(mySubstitutions.values());
  }

  private void addSubstitutions(List<FontSubstitution> substitutions) {
    for (int i = 0; i < substitutions.size(); i++) {
      FontSubstitution nextSubstitution = substitutions.get(i);
      mySubstitutions.put(nextSubstitution.myOriginalFamily, nextSubstitution);
    }
  }

  public Collection<FontSubstitution> getSubstitutions() {
    return Collections.unmodifiableCollection(mySubstitutions.values());
  }

  public FontSubstitution getSubstitution(String originalFamily) {
    return mySubstitutions.get(originalFamily);
  }

  public FontSubstitution getSubstitution(int index) {
    return myIndexedSubstitutions.get(index);
  }

  public List<String> getAvailableSubstitutionFamilies() {
    return myFontCache.getRegisteredFamilies();
  }

  public boolean hasUnresolvedFonts() {
    for (FontSubstitution fs : mySubstitutions.values()) {
      if (!fs.isResolved()) {
        return true;
      }
    }
    return false;
  }
}
