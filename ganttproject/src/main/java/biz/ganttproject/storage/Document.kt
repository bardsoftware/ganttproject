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

import biz.ganttproject.storage.cloud.GPCloudDocument
import biz.ganttproject.storage.cloud.GPCloudOptions
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.hash.Hashing
import com.google.common.io.ByteStreams
import javafx.beans.property.ObjectProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableObjectValue
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.document.ProxyDocument
import org.xml.sax.SAXException
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.CompletableFuture
import javax.xml.parsers.DocumentBuilderFactory


/**
 * @author dbarashev@bardsoftware.com
 */
class DocumentUri(private val components: List<String>,
                  private val isAbsolute: Boolean = true,
                  private val root: String = "/") {

  fun getNameCount(): Int {
    return this.components.size
  }

  fun subpath(start: Int, end: Int): DocumentUri {
    val resultAbsolute = if (start == 0) this.isAbsolute else false
    return DocumentUri(this.components.subList(start, end), resultAbsolute, this.root)
  }

  fun getRoot(): DocumentUri {
    return DocumentUri(listOf(), true, this.root)
  }

  fun getName(idx: Int): String {
    return this.components[idx]
  }

  fun getFileName(): String {
    return this.components.last()
  }

  fun getParent(): DocumentUri {
    if (this.components.isEmpty()) {
      return this
    }
    return DocumentUri(this.components.subList(0, this.components.size - 1), this.isAbsolute, this.root)
  }

  fun resolve(name: String): DocumentUri {
    if (name == "" || name == ".") {
      return this
    }
    if (name == "..") {
      return getParent()
    }
    return DocumentUri(this.components.toMutableList().apply {
      add(name)
      toList()
    }, this.isAbsolute, this.root)
  }

  fun resolve(path: DocumentUri): DocumentUri {
    if (path.isAbsolute) {
      return path
    }
    var result = this
    for (idx in 0 until path.getNameCount()) {
      result = result.resolve(path.getName(idx))
    }
    return result
  }

  fun normalize(): DocumentUri {
    return this.getRoot().resolve(DocumentUri(this.components, false, this.root))
  }

  fun getRootName(): String {
    return this.root
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DocumentUri

    if (components != other.components) return false
    if (isAbsolute != other.isAbsolute) return false
    if (root != other.root) return false

    return true
  }

  override fun hashCode(): Int {
    var result = components.hashCode()
    result = 31 * result + isAbsolute.hashCode()
    result = 31 * result + root.hashCode()
    return result
  }

  override fun toString(): String {
    return components.joinToString(
        separator = "/",
        prefix = "/"
    )
  }


  companion object LocalDocument {
    fun toFile(path: DocumentUri): File {
      val filePath = Paths.get(path.root, *path.components.toTypedArray())
      return filePath.toFile()
    }

    fun createPath(file: File): DocumentUri {
      return createPath(file.toPath())
    }

    fun createPath(pathAsString: String): DocumentUri {
      return createPath(Paths.get(pathAsString))
    }

    private fun createPath(filePath: java.nio.file.Path): DocumentUri {
      val isAbsolute = filePath.isAbsolute
      val root = if (filePath.isAbsolute) filePath.root.toString() else ""

      val components = mutableListOf<String>()
      for (idx in 0 until filePath.nameCount) {
        components.add(filePath.getName(idx).toString())
      }
      return DocumentUri(components, isAbsolute, root)
    }
  }
}

data class LockStatus(val locked: Boolean,
                      val lockOwnerName: String? = null,
                      val lockOwnerEmail: String? = null,
                      val lockOwnerId: String? = null,
                      val raw: JsonNode? = null) {
  val lockedBySomeone: Boolean get() = locked && (lockOwnerId != GPCloudOptions.userId.value)
  val lockExpiration: Long get() = raw?.get("expirationEpochTs")?.longValue() ?: -1
}

interface LockableDocument {
  fun toggleLocked(duration: Duration?): CompletableFuture<LockStatus>
  fun reloadLockStatus(): CompletableFuture<LockStatus>

  val status: ObservableObjectValue<LockStatus>
}

class NetworkUnavailableException(cause: Exception) : RuntimeException(cause)
class VersionMismatchException(val canOverwrite: Boolean = true) : RuntimeException()
class PaymentRequiredException(msg: String) : RuntimeException(msg) {
  constructor() : this("It appears that your team on GanttProject Cloud have run out of credits. Please contact the team owner to resolve this issue.")
}
class ForbiddenException : RuntimeException()

enum class OnlineDocumentMode {
  ONLINE_ONLY, MIRROR, OFFLINE_ONLY
}

data class FetchResult(val onlineDocument: OnlineDocument,
                       val syncChecksum: String,
                       val syncVersion: Long,
                       val actualChecksum: String,
                       val actualVersion: Long,
                       val body: ByteArray,
                       val updateFxn: (FetchResult) -> Unit = {}) {
  var useMirror: Boolean = false
  fun update() = updateFxn(this)
}

data class LatestVersion(val timestamp: Long, val author: String)

interface OnlineDocument {
  var offlineMirror: Document?
  val isMirrored: ObservableBooleanValue
  val mode: ObjectProperty<OnlineDocumentMode>
  val fetchResultProperty: ObservableObjectValue<FetchResult?>
  val latestVersionProperty: ObservableObjectValue<LatestVersion>

  fun setMirrored(mirrored: Boolean)
  suspend fun fetch(): FetchResult
  suspend fun fetchVersion(version: Long): FetchResult
  fun write(force: Boolean = false)
}

fun (Document).asLocalDocument(): FileDocument? {
  if (this is FileDocument) {
    return this
  }
  if (this is ProxyDocument) {
    if (this.realDocument is FileDocument) {
      return this.realDocument as FileDocument
    }
  }
  return null
}

fun (Document).asOnlineDocument(): OnlineDocument? {
  if (this is ProxyDocument) {
    if (this.realDocument is OnlineDocument) {
      return this.realDocument as OnlineDocument
    }
  } else {
    if (this is GPCloudDocument) {
      return this
    }
  }
  return null
}

fun (Document).checksum(): String? {
  return try {
    Hashing.crc32c().hashBytes(ByteStreams.toByteArray(this.inputStream)).toString()
  } catch (ex: FileNotFoundException) {
    null
  }
}

fun (ByteArray).checksum(): String {
  return Hashing.crc32c().hashBytes(this).toString()
}

fun getDefaultLocalFolder(): File {
  val userHome = File(System.getProperty("user.home"))
  val documents = File(userHome, "Documents")
  return if (!documents.exists() || !documents.canRead()) {
    userHome
  } else {
    val ganttProjectDocs = File(documents, "GanttProject")
    if (ganttProjectDocs.exists()) {
      if (ganttProjectDocs.canWrite()) ganttProjectDocs else documents
    } else {
      ganttProjectDocs.mkdirs()
      if (ganttProjectDocs.exists() && ganttProjectDocs.canWrite()) ganttProjectDocs else documents
    }
  }
}

fun getUserDir() = File(System.getProperty("user.dir"))

private val domParser = DocumentBuilderFactory.newInstance().also {
  it.isValidating = false
  it.isNamespaceAware = false
}

@Throws(SAXException::class)
fun (Document).checkWellFormed() {
  this.inputStream.use {
    domParser.newDocumentBuilder().parse(it)
  }
}
