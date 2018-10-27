/*
Copyright 2018 BarD Software s.r.o

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
package biz.ganttproject.storage

import junit.framework.TestCase
import java.io.File
import java.nio.file.Files

/**
 * @author dbarashev@bardsoftware.com
 */
class DocumentUriTest : TestCase() {
  fun testBasic() {
    val emptyUri = DocumentUri(listOf(), true, "/")
    assertEquals(0, emptyUri.getNameCount())
    assertEquals("/", emptyUri.getRootName())

    val someUri = DocumentUri(listOf("lorem", "ipsum", "dolor"))
    assertEquals(3, someUri.getNameCount())
    assertEquals("dolor", someUri.getFileName())
    assertEquals(DocumentUri(listOf("lorem", "ipsum")), someUri.getParent())
    assertEquals(someUri.getRoot(), someUri.getParent().getParent().getParent())
  }

  fun testResolving() {
    val emptyUri = DocumentUri(listOf(), true, "/")
    assertEquals(DocumentUri(listOf("home"), true), emptyUri.resolve("home"))
    assertEquals(DocumentUri(listOf("home", "ganttproject"), true),
        emptyUri.resolve(DocumentUri(listOf("home", "ganttproject"), false)))
    assertEquals(DocumentUri(listOf("home", "dbarashev"), true),
        DocumentUri(listOf("home", "ganttproject")).resolve(DocumentUri(listOf("..", "dbarashev"), false)))
    assertEquals(DocumentUri(listOf("usr", "share", "ganttproject"), true),
        DocumentUri(listOf("usr", "share")).resolve(DocumentUri(listOf(".", "ganttproject"), false)))
    assertEquals(DocumentUri(listOf("usr", "share", "ganttproject"), true),
        DocumentUri(listOf("home", "ganttproject")).resolve(DocumentUri(listOf("usr", "share", "ganttproject"))))


    assertEquals(DocumentUri(listOf("lorem", "ipsum", "dolor"), false),
        DocumentUri(listOf("lorem", "dolor", "ipsum"), false)
            .resolve("..")
            .resolve(DocumentUri(listOf("..", "ipsum", "dolor"), false)))
  }

  fun testNormalize() {
    assertEquals(DocumentUri(listOf("usr", "share", "ganttproject"), true),
        DocumentUri(listOf("usr", ".", "..", "usr", "share", "", "ganttproject", ".")).normalize())
  }

  fun testFileDocumentUri() {
    val filePath = Files.createTempFile("lorem", "ipsum")
    val documentUri = DocumentUri.createPath(filePath.toFile())
    assertEquals(filePath.fileName.toString(), documentUri.getFileName())
    assertEquals(filePath.nameCount, documentUri.getNameCount())
    assertEquals(filePath.root.toString(), documentUri.getRootName())

    assertEquals(filePath.toFile(), DocumentUri.toFile(documentUri))
    assertEquals(filePath.parent.toFile(), DocumentUri.toFile(documentUri.getParent()))

    val dirPath = Files.createTempDirectory("dir")
    val testTxt = File(dirPath.toFile(), "test.txt")
    val dirUri = DocumentUri.createPath(dirPath.toFile())
    val relativeUri = DocumentUri.createPath("test.txt")
    assertEquals(testTxt, DocumentUri.toFile(dirUri.resolve(relativeUri)))
  }
}