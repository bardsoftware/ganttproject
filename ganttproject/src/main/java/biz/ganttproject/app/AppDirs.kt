/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.app

import net.harawata.appdirs.AppDirsFactory
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.GPVersion
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.io.IOException

/**
 * Returns a working directory (one where application was started from)
 */
fun getUserDir() = File(System.getProperty("user.dir"))

val userHomeDir = File(System.getProperty("user.home").replace("\\", "/")).asWritableDir()

/**
 * This is a directory where GanttProject stores its "application data": updates, calendars.
 */
val appDataDir: File by lazy {
  // First we check if a config directory was specified in the environment variable.
  // This allows for individual customization if required.
  System.getenv("GP_APP_DATA_DIR")?.asWritableDir(tryCreate = true)?.let {
    return@lazy it
  }
  // Second, we check the folder where user.home system property points to, and try to create
  // .ganttproject.d directory inside it.
  userHomeDir?.resolve(".ganttproject.d")?.asWritableDir(tryCreate = true)?.let {
    return@lazy it
  }
  error("Failed to create a config directory.")
}

/**
 * Directory where users can place their calendar files.
 */
val calendarsDir: File? by lazy {
  appDataDir.resolve("calendars").asWritableDir(tryCreate = true).also {
    LOG.info("Reading user calendars from {}", it?.absolutePath ?: "<NOWHERE>")
  }
}


/**
 * Platform-dependent directory to store temporary files for caching or auto-save purposes.
 */
val cacheDir: File? by lazy {
  val fancyFolder = AppDirsFactory.getInstance()
    .getUserCacheDir("GanttProject", GPVersion.getCurrentShortVersionNumber(), "BarD Software")
    .asWritableDir(tryCreate = true)
  if (fancyFolder != null) {
    return@lazy fancyFolder
  }
  // We failed to create a fancy OS-dependent folder, let's try other ways.
  // On Linux we look into /var/tmp
  if (SystemUtils.IS_OS_LINUX) {
    val linuxTmpDir = "/var/tmp".asWritableDir()
    if (linuxTmpDir != null) {
      return@lazy linuxTmpDir
    }
  }
  // We also try using the directory specified in the system property.
  val systemTmpDir = System.getProperty("java.io.tmpdir")?.asWritableDir()
  if (systemTmpDir != null) {
    return@lazy systemTmpDir
  }
  // Finally, let's try to create a temporary file, wherever it is, and use it's containing folder.
  val hackyTmpDir = try {
    File.createTempFile("_ganttproject_autosave", ".empty").parentFile.asWritableDir()
  } catch (e: IOException) {
    LOG.error( "Can't get parent of the temp file", e)
    null
  }
  if (hackyTmpDir != null) {
    return@lazy hackyTmpDir
  }
  LOG.error("Failed to find temporary directory")
  null
}


private fun File.asWritableDir(tryCreate: Boolean = false): File? =
  if (this.exists() && this.isDirectory && this.canWrite()) this
  else {
    if (tryCreate && this.mkdirs()) this else null
  }

private fun String.asWritableDir(tryCreate: Boolean = false): File? = File(this).let {
  if (it.exists() && it.isDirectory && it.canWrite()) it
  else {
    if (tryCreate && it.mkdirs()) it else null
  }
}

private val LOG = GPLogger.create("App.Dirs")