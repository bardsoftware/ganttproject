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
import java.util.*

data class GPCloudFileOptions(
    var lockToken: String = "",
    var lockExpiration: String = "",
    var fingerprint: String = "",
    var name: String = "",
    var isCached: Boolean = false)
class CloudFileOptions : KeyValueOption("files") {
  val files = mutableMapOf<String, GPCloudFileOptions>()

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
          kv.filterValues { value -> value != "" }
        }.flatMap {
          it.value.entries
        }
  }
}

// Persistently stored options
object GPCloudOptions {
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
  val websocketAuthToken: String get() = Base64.getEncoder().encodeToString(
      "${this.userId.value}:${GPCloudOptions.authToken.value}".toByteArray())

  var websocketToken: String? = null
  val cloudFiles = CloudFileOptions()
  val optionGroup: GPOptionGroup = GPOptionGroup("ganttproject-cloud", authToken, validity, userId, cloudFiles)
}

