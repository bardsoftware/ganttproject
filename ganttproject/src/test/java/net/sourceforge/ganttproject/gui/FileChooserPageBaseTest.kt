/*
Copyright 2026 Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject.gui

import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.core.option.FileExtensionFilter
import biz.ganttproject.core.option.ObservableString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.sourceforge.ganttproject.PluginPreferencesImpl
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.osgi.service.prefs.Preferences
import java.io.File

/**
 * Tests the error reporting of the file chooser page which is shared by the import and export wizards.
 *
 * The page proposes the file name of the currently opened project as the default choice. That name is
 * relative, so whether it resolves to an existing file or not depends on the working directory of the
 * process. When the page allows choosing many files, the proposed file is not shown in the UI and
 * can't be corrected by the user, so an error message about it leaves the wizard stuck.
 */
class FileChooserPageBaseTest {
  @Test
  fun `a file list page activates without an error message`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val page = createPage(allowMultipleChoice = true)

      page.setActive(true)

      assertNull(page.errorMessage.value,
        "the page reports an error about a file that the user has not chosen and can't correct")
    }
  }

  @Test
  fun `a file list page does not pick the proposed file`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val page = createPage(allowMultipleChoice = true)

      page.setActive(true)

      assertNull(page.fxFile.value,
        "the page silently holds a single file that the file list in the UI does not show")
    }
  }

  @Test
  fun `a file list page forgets an error message of the previous page`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val page = createPage(allowMultipleChoice = true)
      page.errorMessage.value = "File does not exist"

      page.setActive(true)

      assertNull(page.errorMessage.value, "a stale error message keeps blocking the wizard")
    }
  }

  @Test
  fun `a single file page still reports a missing file`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val page = createPage(allowMultipleChoice = false)

      page.setActive(true)

      assertFalse(page.errorMessage.value.isNullOrBlank(),
        "a single missing file must still be reported, it is shown in the UI and can be corrected")
    }
  }

  private fun createPage(allowMultipleChoice: Boolean): TestFileChooserPage {
    val missing = File(MISSING_FILE_NAME)
    assertFalse(missing.exists(), "precondition: ${missing.absolutePath} must not exist")
    return TestFileChooserPage(FileDocument(File(File("/tmp"), MISSING_FILE_NAME))).also {
      it.allowMultipleChoice = allowMultipleChoice
    }
  }
}

private const val MISSING_FILE_NAME = "file-chooser-page-base-test-no-such-project.gan"

private class TestFileChooserPage(document: Document) : FileChooserPageBase(
  document,
  fileChooserTitle = "Choose a file",
  pageTitle = "Choose a file",
  errorMessage = ObservableString("errorMessage"),
  coroutineScope = CoroutineScope(Dispatchers.Unconfined)
) {
  override val preferences: Preferences = PluginPreferencesImpl(null, "test")
  override val optionGroups: List<GPOptionGroup> = emptyList()
  override fun createFileFilter(): FileExtensionFilter? = null
}
