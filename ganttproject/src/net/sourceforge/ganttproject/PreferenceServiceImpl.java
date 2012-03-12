/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.Preferences;

public class PreferenceServiceImpl implements IPreferencesService {

  @Override
  public IStatus applyPreferences(IExportedPreferences preferences) throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void applyPreferences(IEclipsePreferences node, IPreferenceFilter[] filters) throws CoreException {
    // TODO Auto-generated method stub

  }

  @Override
  public IStatus exportPreferences(IEclipsePreferences node, OutputStream output, String[] excludesList)
      throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void exportPreferences(IEclipsePreferences node, IPreferenceFilter[] filters, OutputStream output)
      throws CoreException {
    // TODO Auto-generated method stub

  }

  @Override
  public String get(String key, String defaultValue, Preferences[] nodes) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean getBoolean(String qualifier, String key, boolean defaultValue, IScopeContext[] contexts) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public byte[] getByteArray(String qualifier, String key, byte[] defaultValue, IScopeContext[] contexts) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getDefaultLookupOrder(String qualifier, String key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getDouble(String qualifier, String key, double defaultValue, IScopeContext[] contexts) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public float getFloat(String qualifier, String key, float defaultValue, IScopeContext[] contexts) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getInt(String qualifier, String key, int defaultValue, IScopeContext[] contexts) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long getLong(String qualifier, String key, long defaultValue, IScopeContext[] contexts) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String[] getLookupOrder(String qualifier, String key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IEclipsePreferences getRootNode() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getString(String qualifier, String key, String defaultValue, IScopeContext[] contexts) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IStatus importPreferences(InputStream input) throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IPreferenceFilter[] matches(IEclipsePreferences node, IPreferenceFilter[] filters) throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IExportedPreferences readPreferences(InputStream input) throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setDefaultLookupOrder(String qualifier, String key, String[] order) {
    // TODO Auto-generated method stub

  }

}
