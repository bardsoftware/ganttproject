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
package net.sourceforge.ganttproject.chart;

/*

 Copyright 2001,2003  The Apache Software Foundation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * A simple class implemented the <code>RenderedImage</code> interface. Only the
 * <code>getTile()</code> method needs to be implemented by subclasses. The
 * instance variables must also be filled in properly.
 * 
 * <p>
 * Normally in JAI <code>PlanarImage</code> is used for this purpose, but in the
 * interest of modularity the use of <code>PlanarImage</code> has been avoided.
 */
public abstract class SimpleRenderedImage implements RenderedImage {

  /** The X coordinate of the image's upper-left pixel. */
  protected int minX;

  /** The Y coordinate of the image's upper-left pixel. */
  protected int minY;

  /** The image's width in pixels. */
  protected int width;

  /** The image's height in pixels. */
  protected int height;

  /** The width of a tile. */
  protected int tileWidth;

  /** The height of a tile. */
  protected int tileHeight;

  /** The X coordinate of the upper-left pixel of tile (0, 0). */
  protected int tileGridXOffset = 0;

  /** The Y coordinate of the upper-left pixel of tile (0, 0). */
  protected int tileGridYOffset = 0;

  /** The image's SampleModel. */
  protected SampleModel sampleModel = null;

  /** The image's ColorModel. */
  protected ColorModel colorModel = null;

  /** The image's sources, stored in a Vector. */
  protected Vector<RenderedImage> sources = new Vector<RenderedImage>();

  /** A Hashtable containing the image properties. */
  protected Hashtable<String, Object> properties = new Hashtable<String, Object>();

  public SimpleRenderedImage() {
  }

  /** Returns the X coordinate of the leftmost column of the image. */
  @Override
  public int getMinX() {
    return minX;
  }

  /**
   * Returns the X coordinate of the column immediately to the right of the
   * rightmost column of the image. getMaxX() is implemented in terms of
   * getMinX() and getWidth() and so does not need to be implemented by
   * subclasses.
   */
  public final int getMaxX() {
    return getMinX() + getWidth();
  }

  /** Returns the X coordinate of the uppermost row of the image. */
  @Override
  public int getMinY() {
    return minY;
  }

  /**
   * Returns the Y coordinate of the row immediately below the bottom row of the
   * image. getMaxY() is implemented in terms of getMinY() and getHeight() and
   * so does not need to be implemented by subclasses.
   */
  public final int getMaxY() {
    return getMinY() + getHeight();
  }

  /** Returns the width of the image. */
  @Override
  public int getWidth() {
    return width;
  }

  /** Returns the height of the image. */
  @Override
  public int getHeight() {
    return height;
  }

  /** Returns a Rectangle indicating the image bounds. */
  public Rectangle getBounds() {
    return new Rectangle(getMinX(), getMinY(), getWidth(), getHeight());
  }

  /** Returns the width of a tile. */
  @Override
  public int getTileWidth() {
    return tileWidth;
  }

  /** Returns the height of a tile. */
  @Override
  public int getTileHeight() {
    return tileHeight;
  }

  /**
   * Returns the X coordinate of the upper-left pixel of tile (0, 0).
   */
  @Override
  public int getTileGridXOffset() {
    return tileGridXOffset;
  }

  /**
   * Returns the Y coordinate of the upper-left pixel of tile (0, 0).
   */
  @Override
  public int getTileGridYOffset() {
    return tileGridYOffset;
  }

  /**
   * Returns the horizontal index of the leftmost column of tiles. getMinTileX()
   * is implemented in terms of getMinX() and so does not need to be implemented
   * by subclasses.
   */
  @Override
  public int getMinTileX() {
    return XToTileX(getMinX());
  }

  /**
   * Returns the horizontal index of the rightmost column of tiles.
   * getMaxTileX() is implemented in terms of getMaxX() and so does not need to
   * be implemented by subclasses.
   */
  public int getMaxTileX() {
    return XToTileX(getMaxX() - 1);
  }

  /**
   * Returns the number of tiles along the tile grid in the horizontal
   * direction. getNumXTiles() is implemented in terms of getMinTileX() and
   * getMaxTileX() and so does not need to be implemented by subclasses.
   */
  @Override
  public int getNumXTiles() {
    return getMaxTileX() - getMinTileX() + 1;
  }

  /**
   * Returns the vertical index of the uppermost row of tiles. getMinTileY() is
   * implemented in terms of getMinY() and so does not need to be implemented by
   * subclasses.
   */
  @Override
  public int getMinTileY() {
    return YToTileY(getMinY());
  }

  /**
   * Returns the vertical index of the bottom row of tiles. getMaxTileY() is
   * implemented in terms of getMaxY() and so does not need to be implemented by
   * subclasses.
   */
  public int getMaxTileY() {
    return YToTileY(getMaxY() - 1);
  }

  /**
   * Returns the number of tiles along the tile grid in the vertical direction.
   * getNumYTiles() is implemented in terms of getMinTileY() and getMaxTileY()
   * and so does not need to be implemented by subclasses.
   */
  @Override
  public int getNumYTiles() {
    return getMaxTileY() - getMinTileY() + 1;
  }

  /** Returns the SampleModel of the image. */
  @Override
  public SampleModel getSampleModel() {
    return sampleModel;
  }

  /** Returns the ColorModel of the image. */
  @Override
  public ColorModel getColorModel() {
    return colorModel;
  }

  /**
   * Gets a property from the property set of this image. If the property name
   * is not recognized, <code>java.awt.Image.UndefinedProperty</code> will be
   * returned.
   * 
   * @param name
   *          the name of the property to get, as a <code>String</code>. @return
   *          a reference to the property <code>Object</code>, or the value
   *          <code>java.awt.Image.UndefinedProperty.</code>
   */
  @Override
  public Object getProperty(String name) {
    name = name.toLowerCase();
    return properties.get(name);
  }

  /**
   * Returns a list of the properties recognized by this image. If no properties
   * are available, <code>null</code> will be returned.
   * 
   * @return an array of <code>String</code>s representing valid property names.
   */
  @Override
  public String[] getPropertyNames() {
    String[] names = new String[properties.size()];
    int index = 0;

    Enumeration<String> e = properties.keys();
    while (e.hasMoreElements()) {
      String name = e.nextElement();
      names[index++] = name;
    }
    return names;
  }

  /**
   * Returns an array of <code>String</code>s recognized as names by this
   * property source that begin with the supplied prefix. If no property names
   * match, <code>null</code> will be returned. The comparison is done in a
   * case-independent manner.
   * 
   * <p>
   * The default implementation calls <code>getPropertyNames()</code> and
   * searches the list of names for matches.
   * 
   * @return an array of <code>String</code>s giving the valid property names.
   */
  public String[] getPropertyNames(String prefix) {
    String propertyNames[] = getPropertyNames();
    if (propertyNames == null) {
      return null;
    }

    prefix = prefix.toLowerCase();

    Vector<String> names = new Vector<String>();
    for (int i = 0; i < propertyNames.length; i++) {
      if (propertyNames[i].startsWith(prefix)) {
        names.addElement(propertyNames[i]);
      }
    }

    if (names.size() == 0) {
      return null;
    }

    // Copy the strings from the Vector over to a String array.
    String prefixNames[] = new String[names.size()];
    int count = 0;
    for (Iterator<String> it = names.iterator(); it.hasNext();) {
      prefixNames[count++] = it.next();
    }

    return prefixNames;
  }

  // Utility methods.

  /**
   * Converts a pixel's X coordinate into a horizontal tile index relative to a
   * given tile grid layout specified by its X offset and tile width.
   */
  public static int XToTileX(int x, int tileGridXOffset, int tileWidth) {
    x -= tileGridXOffset;
    if (x < 0) {
      x += 1 - tileWidth; // Force round to -infinity
    }
    return x / tileWidth;
  }

  /**
   * Converts a pixel's Y coordinate into a vertical tile index relative to a
   * given tile grid layout specified by its Y offset and tile height.
   */
  public static int YToTileY(int y, int tileGridYOffset, int tileHeight) {
    y -= tileGridYOffset;
    if (y < 0) {
      y += 1 - tileHeight; // Force round to -infinity
    }
    return y / tileHeight;
  }

  /**
   * Converts a pixel's X coordinate into a horizontal tile index. This is a
   * convenience method. No attempt is made to detect out-of-range coordinates.
   * 
   * @param x
   *          the X coordinate of a pixel.
   * @return the X index of the tile containing the pixel.
   */
  public int XToTileX(int x) {
    return XToTileX(x, getTileGridXOffset(), getTileWidth());
  }

  /**
   * Converts a pixel's Y coordinate into a vertical tile index. This is a
   * convenience method. No attempt is made to detect out-of-range coordinates.
   * 
   * @param y
   *          the Y coordinate of a pixel.
   * @return the Y index of the tile containing the pixel.
   */
  public int YToTileY(int y) {
    return YToTileY(y, getTileGridYOffset(), getTileHeight());
  }

  /**
   * Converts a horizontal tile index into the X coordinate of its upper left
   * pixel relative to a given tile grid layout specified by its X offset and
   * tile width.
   */
  public static int tileXToX(int tx, int tileGridXOffset, int tileWidth) {
    return tx * tileWidth + tileGridXOffset;
  }

  /**
   * Converts a vertical tile index into the Y coordinate of its upper left
   * pixel relative to a given tile grid layout specified by its Y offset and
   * tile height.
   */
  public static int tileYToY(int ty, int tileGridYOffset, int tileHeight) {
    return ty * tileHeight + tileGridYOffset;
  }

  /**
   * Converts a horizontal tile index into the X coordinate of its upper left
   * pixel. This is a convenience method. No attempt is made to detect
   * out-of-range indices.
   * 
   * @param tx
   *          the horizontal index of a tile.
   * @return the X coordinate of the tile's upper left pixel.
   */
  public int tileXToX(int tx) {
    return tx * tileWidth + tileGridXOffset;
  }

  /**
   * Converts a vertical tile index into the Y coordinate of its upper left
   * pixel. This is a convenience method. No attempt is made to detect
   * out-of-range indices.
   * 
   * @param ty
   *          the vertical index of a tile.
   * @return the Y coordinate of the tile's upper left pixel.
   */
  public int tileYToY(int ty) {
    return ty * tileHeight + tileGridYOffset;
  }

  @Override
  public Vector<RenderedImage> getSources() {
    return null;
  }

  /**
   * Returns the entire image in a single Raster. For images with multiple tiles
   * this will require making a copy.
   * 
   * <p>
   * The returned Raster is semantically a copy. This means that updates to the
   * source image will not be reflected in the returned Raster. For non-writable
   * (immutable) source images, the returned value may be a reference to the
   * image's internal data. The returned Raster should be considered
   * non-writable; any attempt to alter its pixel data (such as by casting it to
   * WritableRaster or obtaining and modifying its DataBuffer) may result in
   * undefined behavior. The copyData method should be used if the returned
   * Raster is to be modified.
   * 
   * @return a Raster containing a copy of this image's data.
   */
  @Override
  public Raster getData() {
    Rectangle rect = new Rectangle(getMinX(), getMinY(), getWidth(), getHeight());
    return getData(rect);
  }

  /**
   * Returns an arbitrary rectangular region of the RenderedImage in a Raster.
   * The rectangle of interest will be clipped against the image bounds.
   * 
   * <p>
   * The returned Raster is semantically a copy. This means that updates to the
   * source image will not be reflected in the returned Raster. For non-writable
   * (immutable) source images, the returned value may be a reference to the
   * image's internal data. The returned Raster should be considered
   * non-writable; any attempt to alter its pixel data (such as by casting it to
   * WritableRaster or obtaining and modifying its DataBuffer) may result in
   * undefined behavior. The copyData method should be used if the returned
   * Raster is to be modified.
   * 
   * @param bounds
   *          the region of the RenderedImage to be returned.
   */
  @Override
  public Raster getData(Rectangle bounds) {
    int startX = XToTileX(bounds.x);
    int startY = YToTileY(bounds.y);
    int endX = XToTileX(bounds.x + bounds.width - 1);
    int endY = YToTileY(bounds.y + bounds.height - 1);
    Raster tile;

    if ((startX == endX) && (startY == endY)) {
      tile = getTile(startX, startY);
      return tile.createChild(bounds.x, bounds.y, bounds.width, bounds.height, bounds.x, bounds.y, null);
    } else {
      // Create a WritableRaster of the desired size
      SampleModel sm = sampleModel.createCompatibleSampleModel(bounds.width, bounds.height);

      // Translate it
      WritableRaster dest = Raster.createWritableRaster(sm, bounds.getLocation());

      for (int j = startY; j <= endY; j++) {
        for (int i = startX; i <= endX; i++) {
          tile = getTile(i, j);
          Rectangle intersectRect = bounds.intersection(tile.getBounds());
          Raster liveRaster = tile.createChild(intersectRect.x, intersectRect.y, intersectRect.width,
              intersectRect.height, intersectRect.x, intersectRect.y, null);
          dest.setDataElements(0, 0, liveRaster);
        }
      }
      return dest;
    }
  }

  /**
   * Copies an arbitrary rectangular region of the RenderedImage into a
   * caller-supplied WritableRaster. The region to be computed is determined by
   * clipping the bounds of the supplied WritableRaster against the bounds of
   * the image. The supplied WritableRaster must have a SampleModel that is
   * compatible with that of the image.
   * 
   * <p>
   * If the raster argument is null, the entire image will be copied into a
   * newly-created WritableRaster with a SampleModel that is compatible with
   * that of the image.
   * 
   * @param dest
   *          a WritableRaster to hold the returned portion of the image.
   * @return a reference to the supplied WritableRaster, or to a new
   *         WritableRaster if the supplied one was null.
   */
  @Override
  public WritableRaster copyData(WritableRaster dest) {
    Rectangle bounds;
    Raster tile;

    if (dest == null) {
      bounds = getBounds();
      Point p = new Point(minX, minY);
      /* A SampleModel to hold the entire image. */
      SampleModel sm = sampleModel.createCompatibleSampleModel(width, height);
      dest = Raster.createWritableRaster(sm, p);
    } else {
      bounds = dest.getBounds();
    }

    int startX = XToTileX(bounds.x);
    int startY = YToTileY(bounds.y);
    int endX = XToTileX(bounds.x + bounds.width - 1);
    int endY = YToTileY(bounds.y + bounds.height - 1);

    for (int j = startY; j <= endY; j++) {
      for (int i = startX; i <= endX; i++) {
        tile = getTile(i, j);
        Rectangle intersectRect = bounds.intersection(tile.getBounds());
        Raster liveRaster = tile.createChild(intersectRect.x, intersectRect.y, intersectRect.width,
            intersectRect.height, intersectRect.x, intersectRect.y, null);

        /*
         * WritableRaster.setDataElements takes into account of inRaster's minX
         * and minY and add these to x and y. Since liveRaster has the origin at
         * the correct location, the following call should not again give these
         * coordinates in places of x and y.
         */
        dest.setDataElements(0, 0, liveRaster);
      }
    }
    return dest;
  }
}
