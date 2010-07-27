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
package net.sourceforge.ganttproject;

import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PluginPreferencesImpl implements Preferences {

    private final String myName;
    private final Preferences myParent;
    private final LinkedHashMap myChildren = new LinkedHashMap();
    private final TreeMap myProps = new TreeMap();
    
    public PluginPreferencesImpl(Preferences parent, String name) {
        myName = name;
        myParent = parent;
    }
    
    public Preferences node(String path) {
        if (path.endsWith("/")) {
            if (!"/".equals(path)) {
                throw new IllegalArgumentException("Path can't end with /");
            }
        }
        if (path.startsWith("/")) {
            if (myParent != null) {
                return myParent.node(path);
            }
            path = path.substring(1);
        }
        if ("".equals(path)) {
            return this;
        }
        int firstSlash = path.indexOf('/');
        String prefix = firstSlash == -1 ? path : path.substring(0, firstSlash);
        String suffix = firstSlash == -1 ? "" : path.substring(firstSlash + 1);
        Preferences child = (Preferences) myChildren.get(prefix);
        if (child==null) {
            child = createChild(prefix);
        }
        return child.node(suffix);
    }

    public void removeNode() throws BackingStoreException {
        throw new UnsupportedOperationException();
    }

    public String absolutePath() {
        return myParent==null ? "/" : myParent.absolutePath() + "/" + myName;
    }

    public String[] childrenNames() throws BackingStoreException {
        return (String[]) myChildren.keySet().toArray(new String[0]);
    }

    public void clear() throws BackingStoreException {
        myProps.clear();
    }

    public void flush() throws BackingStoreException {
    }

    public String get(String key, String def) {
        String value = (String) myProps.get(key);
        return value==null ? def : value; 
    }

    public boolean getBoolean(String key, boolean def) {
        return false;
    }

    public byte[] getByteArray(String key, byte[] def) {
        // TODO Auto-generated method stub
        return null;
    }

    public double getDouble(String key, double def) {
        // TODO Auto-generated method stub
        return 0;
    }

    public float getFloat(String key, float def) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getInt(String key, int def) {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getLong(String key, long def) {
        // TODO Auto-generated method stub
        return 0;
    }

    public String[] keys() throws BackingStoreException {
        return (String[]) myProps.keySet().toArray(new String[0]);
    }

    public String name() {
        return myName;
    }

    public boolean nodeExists(String pathName) throws BackingStoreException {
        return node(pathName)!=null;
    }

    public Preferences parent() {
        return myParent;
    }

    public void put(String key, String value) {
        myProps.put(key, value);
    }

    public void putBoolean(String key, boolean value) {
        // TODO Auto-generated method stub
        
    }

    public void putByteArray(String key, byte[] value) {
        // TODO Auto-generated method stub
        
    }

    public void putDouble(String key, double value) {
        // TODO Auto-generated method stub
        
    }

    public void putFloat(String key, float value) {
        // TODO Auto-generated method stub
        
    }

    public void putInt(String key, int value) {
        // TODO Auto-generated method stub
        
    }

    public void putLong(String key, long value) {
        // TODO Auto-generated method stub
        
    }

    public void remove(String key) {
        myProps.remove(key);
    }

    public void sync() throws BackingStoreException {
        throw new UnsupportedOperationException();
    }

    PluginPreferencesImpl createChild(String name) {
        PluginPreferencesImpl child = new PluginPreferencesImpl(this, name);
        myChildren.put(name, child);
        return child;
    }
}
