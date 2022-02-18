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

import org.ganttproject.impex.htmlpdf.fonts.TTFontCache;
import org.osgi.service.prefs.Preferences;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
  private final Map<String, FontSubstitution> mySubstitutions = new LinkedHashMap<>();
  private final ArrayList<FontSubstitution> myIndexedSubstitutions = new ArrayList<>();
  private final ITextStylesheet myStylesheet;
  private final Preferences myPrefs;

  public FontSubstitutionModel(TTFontCache fontCache, ITextStylesheet stylesheet, Preferences prefs) {
    myFontCache = fontCache;
    myStylesheet = stylesheet;
    myPrefs = prefs;
  }

  public void init() {
    List<FontSubstitution> unresolvedFonts = new ArrayList<>();
    List<FontSubstitution> resolvedFonts = new ArrayList<>();
    for (String nextFamily : myStylesheet.getFontFamilies()) {
      FontSubstitution fs = new FontSubstitution(nextFamily, myPrefs, myFontCache);
      Font awtFont = fs.getSubstitutionFont();
      if (awtFont == null) {
        unresolvedFonts.add(fs);
      } else {
        resolvedFonts.add(fs);
      }
    }
    addSubstitutions(unresolvedFonts);
    addSubstitutions(resolvedFonts);
    myIndexedSubstitutions.addAll(mySubstitutions.values());
  }

  private void addSubstitutions(List<FontSubstitution> substitutions) {
    for (FontSubstitution nextSubstitution : substitutions) {
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
