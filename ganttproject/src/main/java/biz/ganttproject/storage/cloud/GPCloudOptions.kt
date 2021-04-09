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
package biz.ganttproject.storage.cloud

import biz.ganttproject.core.option.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import java.util.*

data class GPCloudFileOptions(
    var lockToken: String = "",
    var lockExpiration: String = "",
    var fingerprint: String = "",
    var name: String = "",
    var offlineMirror: String? = null,
    var lastOnlineVersion: String? = null,
    var lastOnlineChecksum: String? = null,
    var projectRefid: String = "",
    var teamName: String = "",
    var onlineOnly: String = "") {
  fun clearOfflineMirror() {
    this.name = ""
    this.offlineMirror = null
    this.lastOnlineChecksum = null
    this.lastOnlineVersion = null
    this.projectRefid = ""
    this.teamName = ""
    this.onlineOnly = "true"
  }
}

class CloudFileOptions : KeyValueOption("files") {
  val files = mutableMapOf<String, GPCloudFileOptions>()

  override fun loadPersistentValue(value: String?) {
    super.loadPersistentValue(value)
    val sortedMap = keyValueMap.toSortedMap()
    for ((k, v) in sortedMap) {
      val (fp, property) = k.split(delimiters = *arrayOf("."), limit = 2)
      val options = this.files.getOrPut(fp) {
        GPCloudFileOptions(fingerprint = fp)
      }
      when (property) {
        "lockToken" -> options.lockToken = v
        "lockExpiration" -> options.lockExpiration = v
        "name" -> options.name = v
        "onlineOnly" -> options.onlineOnly = v
        "sync.offlinePath" -> options.offlineMirror = v
        "sync.onlineVersion" -> options.lastOnlineVersion = v
        "sync.onlineChecksum" -> options.lastOnlineChecksum = v
        "projectRefid" -> options.projectRefid = v
        "teamName" -> options.teamName = v
      }
    }
  }

  override fun setValueIndex(idx: Int) {
    error("This method is not implemented")
  }

  override fun addValue(value: MutableMap.MutableEntry<String, String>?) {
    error("This method is not implemented")
  }

  override fun updateValue(oldValue: MutableMap.MutableEntry<String, String>?, newValue: MutableMap.MutableEntry<String, String>?) {
    error("This method is not implemented")
  }

  override fun removeValueIndex(idx: Int) {
    error("This method is not implemented")
  }

  override fun asEnumerationOption(): EnumerationOption {
    error("This method is not implemented")
  }

  fun save() {
    this.values =
        this.files.mapValues {
          val kv = mutableMapOf<String, String>()
          kv["${it.value.fingerprint}.name"] = it.value.name
          kv["${it.value.fingerprint}.lockToken"] = it.value.lockToken
          kv["${it.value.fingerprint}.lockExpiration"] = it.value.lockExpiration
          kv["${it.value.fingerprint}.sync.offlinePath"] = it.value.offlineMirror ?: ""
          kv["${it.value.fingerprint}.sync.onlineVersion"] = it.value.lastOnlineVersion ?: ""
          kv["${it.value.fingerprint}.sync.onlineChecksum"] = it.value.lastOnlineChecksum ?: ""
          kv["${it.value.fingerprint}.projectRefid"] = it.value.projectRefid
          kv["${it.value.fingerprint}.teamName"] = it.value.teamName
          kv["${it.value.fingerprint}.onlineOnly"] = it.value.onlineOnly

          kv.filterValues { value -> value != "" }
        }.flatMap {
          it.value.entries
        }
  }

  fun getFileOptions(fp: String): GPCloudFileOptions {
    return this.files.getOrPut(fp) { GPCloudFileOptions(fingerprint = fp) }
  }
}

enum class CloudStatus {UNKNOWN, DISCONNECTED, CONNECTED}

// Persistently stored options
object GPCloudOptions {
  val cloudStatus = SimpleObjectProperty(CloudStatus.UNKNOWN)
  val authToken: StringOption = object : DefaultStringOption("authToken", "") {
    override fun getPersistentValue(): String? {
      return GPCloudOptions.validity.value.toLongOrNull()?.let {
        if (it > 0) {
          super.getPersistentValue()
        } else {
          null
        }
      }
    }
  }
  val defaultOfflineMode = DefaultBooleanOption("defaultOfflineMode", true)
  val validity: StringOption = DefaultStringOption("validity", "")
  val userId: StringOption = object : DefaultStringOption("userId") {
    override fun getPersistentValue(): String? {
      return GPCloudOptions.validity.value.toLongOrNull()?.let {
        if (it > 0) {
          super.getPersistentValue()
        } else {
          null
        }
      }
    }
  }
  val websocketAuthToken: String
    get() = Base64.getEncoder().encodeToString(
        "${this.userId.value}:${GPCloudOptions.authToken.value}".toByteArray())

  var websocketToken: String? = null
  val cloudFiles = CloudFileOptions()
  val optionGroup: GPOptionGroup = GPOptionGroup("ganttproject-cloud", authToken, defaultOfflineMode, validity, userId, cloudFiles)
}

