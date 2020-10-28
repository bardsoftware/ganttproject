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
package net.sourceforge.ganttproject;

import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PluginPreferencesImpl implements Preferences {

  private final String myName;
  private final Preferences myParent;
  private final LinkedHashMap<String, PluginPreferencesImpl> myChildren = new LinkedHashMap<String, PluginPreferencesImpl>();
  private final TreeMap<String, String> myProps = new TreeMap<String, String>();

  public PluginPreferencesImpl(Preferences parent, String name) {
    myName = name;
    myParent = parent;
  }

  @Override
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
    Preferences child = myChildren.get(prefix);
    if (child == null) {
      child = createChild(prefix);
    }
    return child.node(suffix);
  }

  @Override
  public void removeNode() throws BackingStoreException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String absolutePath() {
    return myParent == null ? "/" : myParent.absolutePath() + "/" + myName;
  }

  @Override
  public String[] childrenNames() throws BackingStoreException {
    return myChildren.keySet().toArray(new String[0]);
  }

  @Override
  public void clear() throws BackingStoreException {
    myProps.clear();
  }

  @Override
  public void flush() throws BackingStoreException {
  }

  @Override
  public String get(String key, String def) {
    String value = myProps.get(key);
    return value == null ? def : value;
  }

  @Override
  public boolean getBoolean(String key, boolean def) {
    String value = get(key, null);
    if (value == null) {
      return def;
    }
    try {
      return Boolean.parseBoolean(value);
    } catch (Exception e) {
      GPLogger.log(new RuntimeException("Failed to parse value=" + value + " as boolean", e));
      return false;
    }
  }

  @Override
  public byte[] getByteArray(String key, byte[] def) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getDouble(String key, double def) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public float getFloat(String key, float def) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getInt(String key, int def) {
    String value = get(key, null);
    if (value == null) {
      return def;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return def;
    }
  }

  @Override
  public long getLong(String key, long def) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String[] keys() throws BackingStoreException {
    return myProps.keySet().toArray(new String[0]);
  }

  @Override
  public String name() {
    return myName;
  }

  @Override
  public boolean nodeExists(String pathName) throws BackingStoreException {
    return node(pathName) != null;
  }

  @Override
  public Preferences parent() {
    return myParent;
  }

  @Override
  public void put(String key, String value) {
    myProps.put(key, value);
  }

  @Override
  public void putBoolean(String key, boolean value) {
    myProps.put(key, Boolean.toString(value));
  }

  @Override
  public void putByteArray(String key, byte[] value) {
    // TODO Auto-generated method stub
  }

  @Override
  public void putDouble(String key, double value) {
    // TODO Auto-generated method stub
  }

  @Override
  public void putFloat(String key, float value) {
    // TODO Auto-generated method stub
  }

  @Override
  public void putInt(String key, int value) {
    put(key, String.valueOf(value));
  }

  @Override
  public void putLong(String key, long value) {
    // TODO Auto-generated method stub
  }

  @Override
  public void remove(String key) {
    myProps.remove(key);
  }

  @Override
  public void sync() throws BackingStoreException {
    throw new UnsupportedOperationException();
  }

  PluginPreferencesImpl createChild(String name) {
    PluginPreferencesImpl child = new PluginPreferencesImpl(this, name);
    myChildren.put(name, child);
    return child;
  }
}
