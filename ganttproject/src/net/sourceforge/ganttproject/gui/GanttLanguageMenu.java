/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.gui;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;

/**
 * JMenu or JMenuItem holder which changes the menu item text when the language
 * is changed
 */
public class GanttLanguageMenu<T> implements GanttLanguage.Listener {
    private static final GanttLanguage language = GanttLanguage.getInstance();
    private final String myKey;
    private final T myItem;

    /** Adds the language listener to the given menu */
    static public void addListener(JMenu item, String key) {
        // Because the listener is added in the constructor, the created object is not collected by the GC
        new GanttLanguageMenu<JMenu>(item, key);
    }

    /** Adds the language listener to the given menu item */
    static public void addListener(JMenuItem item, String key) {
        // Because the listener is added in the constructor, the created object is not collected by the GC
        new GanttLanguageMenu<JMenuItem>(item, key);
    }

    private GanttLanguageMenu(T item, String key) {
        myKey = key;
        myItem = item;
        languageChanged(null);
        language.addListener(this);
    }

    public void languageChanged(Event event) {
        String label = language.getText(myKey);
        int index = label.indexOf('$');
        if (index != -1 && label.length() - index > 1) {
            setText(label.substring(0, index).concat(label.substring(++index)));
            setMnemonic(Character.toLowerCase(label.charAt(index)));
        } else {
            setText(label);
        }
    }
    
    private void setText(String text) {
        if(myItem instanceof JMenu) {
            ((JMenu) myItem).setText(text);
        } else if(myItem instanceof JMenuItem) {
            ((JMenuItem) myItem).setText(text);
        }
    }

    private void setMnemonic(int mnemonic) {
        if(myItem instanceof JMenu) {
            ((JMenu) myItem).setMnemonic(mnemonic);
        } else if(myItem instanceof JMenuItem) {
            ((JMenuItem) myItem).setMnemonic(mnemonic);
        }
    }
}
