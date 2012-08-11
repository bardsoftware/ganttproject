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

import java.util.Map;

import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.URIException;
import org.apache.webdav.lib.WebdavResource;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavException;

/**
 * Caching factory of WebDavResource instances.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class WebDavResourceSlideFactory {
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

  private final Map<Key, WebDavResourceSlideImpl> myCache = Maps.newHashMap();

  WebDavResourceSlideImpl createResource(String urlString, String username, String password) throws WebDavException {
    Key key = new Key(urlString, username, password);
    WebDavResourceSlideImpl result = myCache.get(key);
    if (result == null) {
      result = new WebDavResourceSlideImpl(urlString, username, password, this);
      myCache.put(key, result);
    }
    return result;
  }

  void clearCache() {
    myCache.clear();
  }

  public WebDavResourceSlideImpl createResource(WebdavResource impl) {
    try {
      HttpURL httpUrl = impl.getHttpURLExceptForUserInfo();
      HttpURL httpUrlUserinfo = impl.getHttpURL();
      Key key = new Key(httpUrl.toString(), httpUrlUserinfo.getUser(), httpUrlUserinfo.getPassword());
      WebDavResourceSlideImpl result = myCache.get(key);
      if (result == null) {
        result = new WebDavResourceSlideImpl(impl, this);
        myCache.put(key, result);
      }
      return result;
    } catch (URIException e) {
      return new WebDavResourceSlideImpl(impl, this);
    }
  }
}
