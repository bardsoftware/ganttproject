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
package org.ganttproject.impex.htmlpdf.fonts;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 07.01.2004
 */
public class FontRecord {
  private URI myLocation;

  private URI myMetricsLocation;

  private ArrayList<FontTriplet> myTriplets = new ArrayList<FontTriplet>();

  private TTFFileExt myTTFFile;

  public FontRecord(File fontFile, FontMetricsStorage metricsStorage) throws IOException {
    myTTFFile = new TTFFileExt(fontFile);
    myLocation = fontFile.toURI();
    myMetricsLocation = metricsStorage.getFontMetricsURI(myTTFFile);
  }

  public FontRecord(URI fontLocation, URI metricsLocation) {
    myLocation = fontLocation;
    myMetricsLocation = metricsLocation;
  }

  public void addTriplet(FontTriplet triplet) {
    myTriplets.add(triplet);
  }

  public URI getFontLocation() {
    return myLocation;
  }

  public URI getMetricsLocation() {
    return myMetricsLocation;
  }

  public FontTriplet[] getFontTriplets() {
    return myTriplets.toArray(new FontTriplet[0]);
  }

  public TTFFileExt getTTFFile() {
    return myTTFFile;
  }

  @Override
  public String toString() {
    return "font file=" + myLocation + " metrics file=" + myMetricsLocation;
  }
}
