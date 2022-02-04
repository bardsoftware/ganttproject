/*
Copyright 2012 GanttProject Team

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
package net.sourceforge.ganttproject.document.webdav

import io.milton.common.Path
import java.util.Locale
import java.net.MalformedURLException
import java.net.URL

/**
 * Encapsulates WebDAV resource location.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class WebDavUri {
  @JvmField
  val hostUrl: String
  @JvmField
  val rootPath: String
  @JvmField
  val path: String
  @JvmField
  val hostName: String
  @JvmField
  val port: Int
  @JvmField
  val isSecure: Boolean

  constructor(fullUrl: String) {
    var tryHostUrl: String
    var tryPath: String
    var tryPort: Int
    var trySecure = false
    try {
      val url = URL(fullUrl)
      tryHostUrl = url.host
      trySecure = "https" == url.protocol.lowercase(Locale.getDefault())
      tryPort = url.port
      tryPath = url.path
    } catch (e: MalformedURLException) {
      tryHostUrl = fullUrl
      tryPort = -1
      tryPath = ""
    }
    hostName = ""
    hostUrl = tryHostUrl
    port = tryPort
    rootPath = ""
    path = tryPath
    isSecure = trySecure
  }

  constructor(hostName: String, hostUrl: String, path: String) {
    assert(!hostUrl.endsWith("/"))
    assert(path.isEmpty() || path.startsWith("/"))
    this.hostName = hostName
    var tryHostUrl: String
    var tryRootPath: String
    var tryPort: Int
    var trySecure = false
    try {
      val url = URL(hostUrl)
      tryHostUrl = url.host
      trySecure = "https" == url.protocol.lowercase(Locale.getDefault())
      tryPort = url.port
      tryRootPath = url.path
    } catch (e: MalformedURLException) {
      tryHostUrl = hostUrl
      tryPort = 80
      tryRootPath = ""
    }
    this.hostUrl = tryHostUrl
    port = tryPort
    rootPath = tryRootPath
    this.path = path
    isSecure = trySecure
  }

  fun buildUrl(): String {
    return buildRootUrl() + path
  }

  fun buildRootUrl(): String =
    if (hostUrl.isBlank()) {
      ""
    } else {
      val scheme = if (isSecure) "https://" else "http://"
      val port = if (port == -1 || port == 80) "" else ":$port"
      "$scheme$hostUrl$port$rootPath"
    }

  fun buildParent(): WebDavUri {
    return WebDavUri(hostName, buildRootUrl(), Path.path(path).parent.toString())
  }

  fun buildChild(name: String?): WebDavUri {
    return WebDavUri(hostName, buildRootUrl(), Path.path(path).child(name).toString())
  }
}
