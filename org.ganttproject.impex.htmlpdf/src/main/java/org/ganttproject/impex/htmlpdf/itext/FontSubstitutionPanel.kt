/*
Copyright 2003-2026 Dmitry Barashev, BarD Software s.r.o.

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

package org.ganttproject.impex.htmlpdf.itext

import biz.ganttproject.lib.fx.GPTableCell
import biz.ganttproject.lib.fx.vbox
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.util.Callback
import org.controlsfx.control.tableview2.TableView2
import org.ganttproject.impex.htmlpdf.itext.FontSubstitutionModel.FontSubstitution

/**
 * Represents a UI panel used for managing font substitutions.
 *
 * The panel displays a table containing theme fonts and corresponding substitutions,
 * allowing users to define or modify substitutions for unresolved fonts.
 *
 * @param myModel The data model that holds font substitutions and their resolution state.
 * @param errorMessage A binding property to hold error messages regarding unresolved fonts.
 */
class FontSubstitutionPanel(private val myModel: FontSubstitutionModel, val errorMessage: SimpleStringProperty) {

    fun getComponentFx(): Parent {
        val substitutions = FXCollections.observableArrayList(myModel.substitutions)
        val table = TableView2(substitutions).also { it.styleClass.add("font-substitution-table") }
        table.isEditable = true

        val themeFontColumn = TableColumn<FontSubstitution, String>("Theme font")
        themeFontColumn.cellValueFactory = Callback { features -> SimpleStringProperty(features.value.myOriginalFamily) }
        themeFontColumn.setCellFactory { FontSubstitutionCell(false) }

        val substitutionColumn = TableColumn<FontSubstitution, String>("Substitution")
        substitutionColumn.cellValueFactory = Callback { features -> SimpleStringProperty(features.value.substitutionFamily) }
        substitutionColumn.setCellFactory {
            FontSubstitutionCell(true, myModel.availableSubstitutionFamilies)
        }
        substitutionColumn.onEditCommit = EventHandler { event ->
            val substitution = event.rowValue
            substitution.substitutionFamily = event.newValue
            updateFontStatusMessage()
            table.refresh() // Refresh to update rendering of other columns if necessary
        }

        table.columns.addAll(themeFontColumn, substitutionColumn)
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

        updateFontStatusMessage()

        return vbox {
          addClasses("font-substitution-panel")
            add(table, alignment = null, growth = Priority.ALWAYS)
        }
    }

    private fun updateFontStatusMessage() {
        if (myModel.hasUnresolvedFonts()) {
          errorMessage.value = "Some fonts used in the selected theme have not been found\nYou may define substitutions in the table"
        } else {
          errorMessage.value = ""
        }
    }
}


private class FontSubstitutionCell(val isSubstitutionColumn: Boolean, val availableFamilies: List<String>? = null) : GPTableCell<FontSubstitution, String>() {
  private var comboBox: ComboBox<String>? = null

  override fun updateItem(item: String?, empty: Boolean) {
    whenNotEmpty(item, empty) {
      val substitution = tableView.items[index]
      if (isEditing && isSubstitutionColumn) {
        graphic = comboBox ?: createDropdown().also {
          it.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            commitEdit(newValue)
          }
          comboBox = it
        }
        text = null
      } else {
        text = item
        graphic = null

        // Apply font and color
        val awtFont = if (!substitution.isResolved || !isSubstitutionColumn) {
          null
        } else {
          substitution.substitutionFont
        }

        if (awtFont != null) {
          font = Font.font(awtFont.family, 16.0)
        } else {
          font = Font.font(16.0)
        }

        if (substitution.isResolved) {
          textFill = Color.BLACK
        } else {
          textFill = Color.RED
        }
      }
    }
  }
  override fun startEdit() {
    if (!isSubstitutionColumn) return
    super.startEdit()
    graphic = comboBox ?: createDropdown().also { comboBox = it }
    text = null
  }

  override fun cancelEdit() {
    super.cancelEdit()
    text = item
    graphic = null
  }

  private fun createDropdown() = ComboBox(FXCollections.observableArrayList(availableFamilies)).also { comboBox ->
    comboBox.maxWidth = Double.MAX_VALUE
    comboBox.setOnAction {
      commitEdit(comboBox.selectionModel.selectedItem)
    }
    comboBox.selectionModel.select(item)
  }
}

