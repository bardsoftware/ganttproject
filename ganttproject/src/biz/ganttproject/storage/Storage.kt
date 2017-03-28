/*
Copyright 2017 Dmitry Barashev, BarD Software s.r.o

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

import java.io.File

/**
 * @author dbarashev@bardsoftware.com
 */
sealed class StorageMode(val name: String) {

    class FileException: Exception {
        val args: Array<out Any>
        constructor(message: String, vararg args: Any) : super(message) {
            this.args = args
        }

    }

    abstract fun tryFile(file: File): Unit

    class Open: StorageMode("open") {
        override fun tryFile(file: File) {
            if (file.exists() && file.isDirectory) {
                throw FileException("document.storage.error.read.directory", file)
            }
            if (!file.exists()) {
                throw FileException("document.storage.error.read.notExists", file)
            }
            if (file.exists() && !file.canRead()) {
                throw FileException("document.storage.error.read.cantRead", file)
            }
        }

    }

    class Save: StorageMode("save") {
        override fun tryFile(file: File) {
            if (file.exists() && file.isDirectory) {
                throw FileException("document.storage.error.write.isDirectory", file)
            }
            if (file.exists() && !file.canWrite()) {
                throw FileException("document.storage.error.write.cantOverwrite", file)
            }
            if (!file.exists() && !file.parentFile.exists()) {
                throw FileException("document.storage.error.write.parentNotExists", file, file.parentFile)
            }
            if (!file.exists() && file.parentFile.exists() && !file.parentFile.canWrite()) {
                throw FileException("document.storage.error.parentNotWritable", file, file.parentFile)
            }
        }

    }
}