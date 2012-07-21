/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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
package net.sourceforge.ganttproject.document.webdav;


import io.milton.httpclient.Host;

import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

/**
 * Caching factory of WebDavResource instances.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class MiltonResourceFactory {
  private static final int TIMEOUT_MS = 30000;
  private static class Key {
    String url;
    String username;
    String password;

    Key(String url, String username, String password) {
      this.url = url;
      this.username = username;
      this.password = password;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(url, username, password);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Key == false) {
        return false;
      }
      Key that = (Key) obj;
      return Objects.equal(this.url, that.url) && Objects.equal(this.username, that.username) && Objects.equal(this.password, that.password);
    }
  }

  private final Map<String, Host> myHostCache = Maps.newHashMap();
  private final Map<Key, MiltonResourceImpl> myResourceCache = Maps.newHashMap();
  private String myUsername;
  private String myPassword;

  public MiltonResourceFactory() {}

  public MiltonResourceFactory(String username, String password) {
    myUsername = username;
    myPassword = password;
  }

  MiltonResourceImpl createResource(WebDavUri uri) {
    Key key = new Key(uri.buildUrl(), myUsername, myPassword);
    MiltonResourceImpl result = myResourceCache.get(key);
    if (result == null) {
      result = new MiltonResourceImpl(uri, getHost(uri), this);
      myResourceCache.put(key, result);
    }
    return result;
  }

  void clearCache() {
    myResourceCache.clear();
  }

  public void setCredentials(String username, String password) {
    myUsername = username;
    myPassword = password;
  }

  private Host getHost(WebDavUri uri) {
    String hostKey = uri.buildRootUrl();
    Host result = myHostCache.get(hostKey);
    if (result== null) {
      result = new Host(uri.hostUrl, uri.rootPath, uri.port, myUsername, myPassword, null, TIMEOUT_MS, null, null);
      // myHostCache.put(hostKey, result);
    }
    return result;
  }
}
