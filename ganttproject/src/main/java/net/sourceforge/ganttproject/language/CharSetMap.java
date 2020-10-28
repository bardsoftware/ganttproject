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
package net.sourceforge.ganttproject.language;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * This class maintains a set of mappers defining mappings between locales and
 * the corresponding charsets. The mappings are defined as properties between
 * locale and charset names. The definitions can be listed in property files
 * located in user's home directory, Java home directory or the current class
 * jar. In addition, this class maintains static default mappings and
 * constructors support application specific mappings.
 * 
 * @author <a href="mailto:ilkka.priha@simsoft.fi">Ilkka Priha</a>
 * @version $Id$
 */
public class CharSetMap {
  /**
   * The default charset when nothing else is applicable.
   */
  public static final String DEFAULT_CHARSET = "ISO-8859-1";

  /**
   * The name for charset mapper resources.
   */
  public static final String CHARSET_RESOURCE = "charset.properties";

  /**
   * Priorities of available mappers.
   */
  private static final int MAP_CACHE = 0;
  private static final int MAP_PROG = 1;
  private static final int MAP_HOME = 2;
  private static final int MAP_SYS = 3;
  private static final int MAP_JAR = 4;
  private static final int MAP_COM = 5;

  /**
   * A common charset mapper for languages.
   */
  private static HashMap<String, String> commonMapper = new HashMap<String, String>();
  static {
    commonMapper.put("ar", "ISO-8859-6");
    commonMapper.put("be", "ISO-8859-5");
    commonMapper.put("bg", "ISO-8859-5");
    commonMapper.put("ca", "ISO-8859-1");
    commonMapper.put("cs", "ISO-8859-2");
    commonMapper.put("da", "ISO-8859-1");
    commonMapper.put("de", "ISO-8859-1");
    commonMapper.put("el", "ISO-8859-7");
    commonMapper.put("en", "ISO-8859-1");
    commonMapper.put("es", "ISO-8859-1");
    commonMapper.put("et", "ISO-8859-1");
    commonMapper.put("fi", "ISO-8859-1");
    commonMapper.put("fr", "ISO-8859-1");
    commonMapper.put("hr", "ISO-8859-2");
    commonMapper.put("hu", "ISO-8859-2");
    commonMapper.put("is", "ISO-8859-1");
    commonMapper.put("it", "ISO-8859-1");
    commonMapper.put("iw", "ISO-8859-8");
    commonMapper.put("ja", "Shift_JIS");
    commonMapper.put("ko", "EUC-KR");
    commonMapper.put("lt", "ISO-8859-2");
    commonMapper.put("lv", "ISO-8859-2");
    commonMapper.put("mk", "ISO-8859-5");
    commonMapper.put("nl", "ISO-8859-1");
    commonMapper.put("no", "ISO-8859-1");
    commonMapper.put("pl", "ISO-8859-2");
    commonMapper.put("pt", "ISO-8859-1");
    commonMapper.put("ro", "ISO-8859-2");
    commonMapper.put("ru", "ISO-8859-5");
    commonMapper.put("sh", "ISO-8859-5");
    commonMapper.put("sk", "ISO-8859-2");
    commonMapper.put("sl", "ISO-8859-2");
    commonMapper.put("sq", "ISO-8859-2");
    commonMapper.put("sr", "ISO-8859-5");
    commonMapper.put("sv", "ISO-8859-1");
    commonMapper.put("tr", "ISO-8859-9");
    commonMapper.put("uk", "ISO-8859-5");
    commonMapper.put("zh", "GB2312");
    commonMapper.put("zh_TW", "Big5");
  }

  /**
   * An array of available charset mappers.
   */
  private final Map<String, String> mappers[] = new Map[6];

  /**
   * Loads mappings from a stream.
   * 
   * @param input
   *          an input stream.
   * @return the mappings.
   * @throws IOException
   *           for an incorrect stream.
   */
  protected static Map<String, String> loadStream(InputStream input) throws IOException {
    Properties props = new Properties();
    props.load(input);
    return new HashMap(props);
  }

  /**
   * Loads mappings from a file.
   * 
   * @param file
   *          a file.
   * @return the mappings.
   * @throws IOException
   *           for an incorrect file.
   */
  protected static Map<String, String> loadFile(File file) throws IOException {
    return loadStream(new FileInputStream(file));
  }

  /**
   * Loads mappings from a file path.
   * 
   * @param path
   *          a file path.
   * @return the mappings.
   * @throws IOException
   *           for an incorrect file.
   */
  protected static Map<String, String> loadPath(String path) throws IOException {
    return loadFile(new File(path));
  }

  /**
   * Loads mappings from a resource.
   * 
   * @param name
   *          a resource name.
   * @return the mappings.
   */
  protected static Map<String, String> loadResource(String name) {
    InputStream input = CharSetMap.class.getResourceAsStream(name);
    if (input != null) {
      try {
        return loadStream(input);
      } catch (IOException x) {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * Constructs a new charset map with default mappers.
   */
  public CharSetMap() {
    String path;
    try {
      // Check whether the user directory contains mappings.
      path = System.getProperty("user.home");
      if (path != null) {
        path = path + File.separator + CHARSET_RESOURCE;
        mappers[MAP_HOME] = loadPath(path);
      }
    } catch (Exception x) {
    }

    try {
      // Check whether the system directory contains mappings.
      path = System.getProperty("java.home") + File.separator + "lib" + File.separator + CHARSET_RESOURCE;
      mappers[MAP_SYS] = loadPath(path);
    } catch (Exception x) {
    }

    // Check whether the current class jar contains mappings.
    mappers[MAP_JAR] = loadResource("/META-INF/" + CHARSET_RESOURCE);

    // Set the common mapper to have the lowest priority.
    mappers[MAP_COM] = commonMapper;

    // Set the cache mapper to have the highest priority.
    mappers[MAP_CACHE] = new Hashtable<String, String>();
  }

  /**
   * Constructs a charset map from properties.
   * 
   * @param props
   *          charset mapping properties.
   */
  public CharSetMap(Properties props) {
    this();
    mappers[MAP_PROG] = new HashMap(props);
  }

  /**
   * Constructs a charset map read from a stream.
   * 
   * @param input
   *          an input stream.
   * @throws IOException
   *           for an incorrect stream.
   */
  public CharSetMap(InputStream input) throws IOException {
    this();
    mappers[MAP_PROG] = loadStream(input);
  }

  /**
   * Constructs a charset map read from a property file.
   * 
   * @param file
   *          a property file.
   * @throws IOException
   *           for an incorrect property file.
   */
  public CharSetMap(File file) throws IOException {
    this();
    mappers[MAP_PROG] = loadFile(file);
  }

  /**
   * Constructs a charset map read from a property file path.
   * 
   * @param path
   *          a property file path.
   * @throws IOException
   *           for an incorrect property file.
   */
  public CharSetMap(String path) throws IOException {
    this();
    mappers[MAP_PROG] = loadPath(path);
  }

  /**
   * Sets a locale-charset mapping.
   * 
   * @param key
   *          the key for the charset.
   * @param charset
   *          the corresponding charset.
   */
  public synchronized void setCharSet(String key, String charset) {
    HashMap<String, String> mapper = (HashMap<String, String>) mappers[MAP_PROG];
    mapper = mapper != null ? (HashMap<String, String>) mapper.clone() : new HashMap<String, String>();
    mapper.put(key, charset);
    mappers[MAP_PROG] = mapper;
    mappers[MAP_CACHE].clear();
  }

  /**
   * Gets the charset for a locale. First a locale specific charset is searched
   * for, then a country specific one and lastly a language specific one. If
   * none is found, the default charset is returned.
   * 
   * @param locale
   *          the locale.
   * @return the charset.
   */
  public String getCharSet(Locale locale) {
    // Check the cache first.
    String key = locale.toString();
    if (key.length() == 0) {
      key = "__" + locale.getVariant();
      if (key.length() == 2) {
        return DEFAULT_CHARSET;
      }
    }
    String charset = searchCharSet(key);
    if (charset.length() == 0) {
      // Not found, perform a full search and update the cache.
      String[] items = new String[3];
      items[2] = locale.getVariant();
      items[1] = locale.getCountry();
      items[0] = locale.getLanguage();
      charset = searchCharSet(items);
      if (charset.length() == 0) {
        charset = DEFAULT_CHARSET;
      }
      mappers[MAP_CACHE].put(key, charset);
    }
    return charset;
  }

  /**
   * Gets the charset for a locale with a variant. The search is performed in
   * the following order: "lang"_"country"_"variant"="charset",
   * _"counry"_"variant"="charset", "lang"__"variant"="charset",
   * __"variant"="charset", "lang"_"country"="charset", _"country"="charset",
   * "lang"="charset". If nothing of the above is found, the default charset is
   * returned.
   * 
   * @param locale
   *          the locale.
   * @param variant
   *          a variant field.
   * @return the charset.
   */
  public String getCharSet(Locale locale, String variant) {
    // Check the cache first.
    if ((variant != null) && (variant.length() > 0)) {
      String key = locale.toString();
      if (key.length() == 0) {
        key = "__" + locale.getVariant();
        if (key.length() > 2) {
          key += '_' + variant;
        } else {
          key += variant;
        }
      } else if (locale.getCountry().length() == 0) {
        key += "__" + variant;
      } else {
        key += '_' + variant;
      }
      String charset = searchCharSet(key);
      if (charset.length() == 0) {
        // Not found, perform a full search and update the cache.
        String[] items = new String[4];
        items[3] = variant;
        items[2] = locale.getVariant();
        items[1] = locale.getCountry();
        items[0] = locale.getLanguage();
        charset = searchCharSet(items);
        if (charset.length() == 0) {
          charset = DEFAULT_CHARSET;
        }
        mappers[MAP_CACHE].put(key, charset);
      }
      return charset;
    } else {
      return getCharSet(locale);
    }
  }

  /**
   * Gets the charset for a specified key.
   * 
   * @param key
   *          the key for the charset.
   * @return the found charset or the default one.
   */
  public String getCharSet(String key) {
    String charset = searchCharSet(key);
    return charset.length() > 0 ? charset : DEFAULT_CHARSET;
  }

  /**
   * Gets the charset for a specified key.
   * 
   * @param key
   *          the key for the charset.
   * @param def
   *          the default charset if none is found.
   * @return the found charset or the given default.
   */
  public String getCharSet(String key, String def) {
    String charset = searchCharSet(key);
    return charset.length() > 0 ? charset : def;
  }

  /**
   * Searches for a charset for a specified locale.
   * 
   * @param items
   *          an array of locale items.
   * @return the found charset or an empty string.
   */
  private String searchCharSet(String[] items) {
    String charset;
    StringBuffer sb = new StringBuffer();
    for (int i = items.length; i > 0; i--) {
      charset = searchCharSet(items, sb, i);
      if (charset.length() > 0) {
        return charset;
      }
      sb.setLength(0);
    }
    return "";
  }

  /**
   * Searches recursively for a charset for a specified locale.
   * 
   * @param items
   *          an array of locale items.
   * @param base
   *          a buffer of base items.
   * @param count
   *          the number of items to go through.
   * @return the found charset or an empty string.
   */
  private String searchCharSet(String[] items, StringBuffer base, int count) {
    if ((--count >= 0) && (items[count] != null) && (items[count].length() > 0)) {
      String charset;
      base.insert(0, items[count]);
      int length = base.length();
      for (int i = count; i > 0; i--) {
        if ((i == count) || (i <= 1)) {
          base.insert(0, '_');
          length++;
        }
        charset = searchCharSet(items, base, i);
        if (charset.length() > 0) {
          return charset;
        }
        base.delete(0, base.length() - length);
      }
      return searchCharSet(base.toString());
    } else {
      return "";
    }
  }

  /**
   * Searches for a charset for a specified key.
   * 
   * @param key
   *          the key for the charset.
   * @return the found charset or an empty string.
   */
  private String searchCharSet(String key) {
    if ((key != null) && (key.length() > 0)) {
      // Go through mappers.
      Map<String, String> mapper;
      String charset;
      for (int i = 0; i < mappers.length; i++) {
        mapper = mappers[i];
        if (mapper != null) {
          charset = mapper.get(key);
          if (charset != null) {
            // Update the cache.
            if (i > MAP_CACHE) {
              mappers[MAP_CACHE].put(key, charset);
            }
            return charset;
          }
        }
      }

      // Not found, add an empty string to the cache.
      mappers[MAP_CACHE].put(key, "");
    }
    return "";
  }

  /**
   * Sets a common locale-charset mapping.
   * 
   * @param key
   *          the key for the charset.
   * @param charset
   *          the corresponding charset.
   */
  protected synchronized void setCommonCharSet(String key, String charset) {
    HashMap<String, String> mapper = new HashMap<String, String>(mappers[MAP_COM]);
    mapper.put(key, charset);
    mappers[MAP_COM] = mapper;
    mappers[MAP_CACHE].clear();
  }
}
