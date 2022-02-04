/*
Copyright 2022 BarD Software s.r.o.
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

import com.google.common.base.Objects
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty

/**
 * Encapsulates server access information.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class WebDavServerDescriptor {
  private val _name = SimpleStringProperty("")
  var name: String
  get() = _name.value
  set(value) { _name.value = value }
  fun nameProperty() = _name

  private var rootUri: WebDavUri? = null
  private val _rootUrl = SimpleStringProperty("")
  var rootUrl: String
    get() = if (rootUri == null) "" else rootUri!!.buildRootUrl()
    set(value) {
      var v = value
      do {
        v = v.removeSuffix("/")
      } while (v.endsWith("/"))
      rootUri = WebDavUri(name, v, "")
      _rootUrl.value = v
    }
  fun rootUrlProperty() = _rootUrl

  private val _username = SimpleStringProperty("")
  var username: String
  get() = _username.value
  set(value) { _username.value = value }
  fun usernameProperty() = _username

  private val _password = SimpleStringProperty("")
  var password: String
  get() = _password.value
  set(value) { _password.value = value }
  fun passwordProperty() = _password

  private val _savePassword = SimpleBooleanProperty(false)
  var savePassword: Boolean
  get() =  _savePassword.value
  set(value) { _savePassword.value = value }
  fun savePasswordProperty() = _savePassword

  constructor() {}
  internal constructor(name: String?, rootUrl: String, username: String?) {
    this.name = name ?: ""
    this.rootUrl = rootUrl
    this.username = username ?: ""
  }

  constructor(name: String?, rootUrl: String, username: String?, password: String?) : this(name, rootUrl, username) {
    this.password = password ?: ""
  }

  override fun equals(obj: Any?): Boolean = (obj as? WebDavServerDescriptor)?.let {
    Objects.equal(this.rootUrl, it.rootUrl)
  } ?: false

  override fun hashCode(): Int {
    return Objects.hashCode(rootUrl)
  }

  fun clone(): WebDavServerDescriptor {
    val result = WebDavServerDescriptor(name, rootUrl, username, password)
    result.savePassword = savePassword
    return result
  }
}

