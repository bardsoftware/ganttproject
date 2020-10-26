/*
Copyright 2020 BarD Software s.r.o

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
package biz.ganttproject.storage.webdav

import biz.ganttproject.lib.fx.vbox
import biz.ganttproject.storage.StorageUi
import javafx.beans.property.StringProperty
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.PasswordField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.control.PropertySheet
import org.controlsfx.property.BeanProperty
import org.controlsfx.property.editor.AbstractPropertyEditor
import org.controlsfx.property.editor.PropertyEditor
import java.beans.IntrospectionException
import java.beans.PropertyDescriptor
import java.util.*
import java.util.function.Consumer

/**
 * This is a UI for editing WebDAV server properties.
 *
 * @author dbarashev@bardsoftware.com
 */
class WebdavServerSetupPane(
    webdavServer: WebDavServerDescriptor,
    private val onDone: Consumer<WebDavServerDescriptor?>,
    private val hasDelete: Boolean) : StorageUi {
  private val myWebdavServer: WebDavServerDescriptor = webdavServer.clone()

  override val id: String
    get() = "webdav-setup"

  override val category: String
    get() = ""
  override val name: String
    get() = ""

  override fun createUi(): Pane {
    return try {
      doCreateUi()
    } catch (e: IntrospectionException) {
      e.printStackTrace()
      Pane()
    }
  }

  @Throws(IntrospectionException::class)
  private fun doCreateUi() = vbox {
    vbox.styleClass.add("pane-service-contents")
    vbox.stylesheets.add("/biz/ganttproject/storage/StorageDialog.css")

    addTitle(if (hasDelete) "webdav.ui.title.editServer" else "webdav.ui.title.newServer").also {
      it.styleClass.add("title-integrated")
    }

    add(PropertySheet().apply {
      styleClass.addAll("property-sheet")
      isModeSwitcherVisible = false
      isSearchBoxVisible = false
      items.add(BeanProperty(
          myWebdavServer, WebDavPropertyDescriptor("name", "webdav.serverName")))
      items.add(BeanProperty(
          myWebdavServer, WebDavPropertyDescriptor("rootUrl", "option.webdav.server.url.label")))
      items.add(BeanProperty(
          myWebdavServer, WebDavPropertyDescriptor("username", "option.webdav.server.username.label")))
      items.add(object : BeanProperty(
          myWebdavServer, WebDavPropertyDescriptor("password", "option.webdav.server.password.label")) {
        override fun getPropertyEditorClass(): Optional<Class<out PropertyEditor<*>?>> {
          return Optional.of(PasswordPropertyEditor::class.java)
        }
      })
      items.add(BeanProperty(
          myWebdavServer, WebDavPropertyDescriptor("savePassword", "option.webdav.server.savePassword.label.trailing")))

    }, alignment = null, growth = Priority.ALWAYS)

    add(HBox().apply {
      styleClass.add("doclist-save-box")
      if (hasDelete) {
        children.add(Button(i18n.formatText("delete")).apply {
          addEventHandler(ActionEvent.ACTION) { onDone.accept(null) }
          isFocusTraversable = false
        })
      }
      children.add(Pane().apply {
        HBox.setHgrow(this, Priority.ALWAYS)
      })
      children.add(Button(i18n.formatText("apply")).apply {
        styleClass.add("btn-attention")
        addEventHandler(ActionEvent.ACTION) { onDone() }
      })
    }, alignment = Pos.CENTER_RIGHT, growth = Priority.NEVER)
  }

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty()
  }

  private fun onDone() {
    onDone.accept(myWebdavServer)
  }
}

private class WebDavPropertyDescriptor(propertyName: String, i18nKey: String)
  : PropertyDescriptor(propertyName, WebDavServerDescriptor::class.java) {
  init {
    displayName = GanttLanguage.getInstance().getText(i18nKey)
  }
}

private class PasswordPropertyEditor(property: PropertySheet.Item)
  : AbstractPropertyEditor<String, PasswordField>(property, PasswordField()) {
  override fun getObservableValue(): StringProperty {
    return this.editor!!.textProperty()
  }

  override fun setValue(value: String?) {
    this.editor!!.text = value
  }
}

