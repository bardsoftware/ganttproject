/*
Copyright 2025 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.app

import net.sourceforge.ganttproject.util.PropertiesUtil
import org.eclipse.core.runtime.Platform
import java.io.File
import java.util.*

internal fun createTranslation(locale: Locale): Translation? {
  return if (getAvailableTranslationLocales().contains(locale)) {
    createTranslationFromFile(locale, "i18n_${locale.language.lowercase()}_${locale.country.uppercase()}.properties"
    )
  } else null
//  return Platform.getExtensionRegistry()?.getConfigurationElementsFor("net.sourceforge.ganttproject.l10n")
//    ?.mapNotNull { l10nConfig ->
//      val path = l10nConfig.getAttribute("path")
//      val pluginBundle = Platform.getBundle(l10nConfig.declaringExtension.namespaceIdentifier)
//        ?: error("Can't find plugin bundle for extension=" + l10nConfig.name)
//      try {
//        val control = if (withFallback)
//          ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES)
//        else
//          ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES).also {
//            println("created bundle $it for locale=$locale")
//          }
//        val resourceBundle = ResourceBundle.getBundle(path, locale, pluginBundle.bundleClassLoader, control)
//        println("result=$resourceBundle")
//        if (withFallback || resourceBundle.locale == locale) {
//          createTranslation(locale, resourceBundle)
//        } else {
//          null
//        }
//      } catch (ex: MissingResourceException) {
//        GPLogger.logToLogger(String.format("Can't find bundle: path=%s locale=%s plugin bundle=%s", path, locale, pluginBundle))
//        null
//      }
//    }
//    ?.firstOrNull()
}

private val pattern = Regex("i18n.*\\.properties")
private val cachedFileNames by lazy {
  Platform.getExtensionRegistry()?.getConfigurationElementsFor("net.sourceforge.ganttproject.l10n")?.flatMap { l10nConfig ->
    val path = l10nConfig.getAttribute("path")
    val pluginBundle = Platform.getBundle(l10nConfig.declaringExtension.namespaceIdentifier)
      ?: error("Can't find plugin bundle for extension=" + l10nConfig.name)
    val fileNames: List<File>? = pluginBundle.getResource(path)?.toURI()?.let {
      File(it).listFiles { f: File -> f.name.matches(pattern) }
    }?.toList()
    fileNames ?: listOf()
  } ?: listOf()
}

internal fun getTranslationFileNames(): List<String> = cachedFileNames.map { it.name }


internal fun createTranslation(locale: Locale, resourceBundle: ResourceBundle) =
  Translation(locale) {
    if (resourceBundle.containsKey(it)) resourceBundle.getString(it) else null
  }

var _explicitTranslation: Translation? = null
val defaultTranslation by lazy {
  _explicitTranslation ?: createTranslationFromFile(Locale.ENGLISH, "i18n.properties") ?: Translation(Locale.ENGLISH) { null }
}

internal fun createTranslationFromFile(locale: Locale, fileName: String): Translation? {
  return cachedFileNames.find { it.name == fileName }?.let {
    val properties = Properties()
    properties.load(it.reader(Charsets.UTF_8))
    Translation(locale) {
      properties.getProperty(it, null)
    }
  }
}


fun getAvailableTranslationLocales() = cachedAvailableLocales

private val cachedAvailableLocales by lazy {
  val result = HashSet<Locale>()
  val translationFileNames = getTranslationFileNames()
  println(translationFileNames)
  Locale.getAvailableLocales().sortedBy { it.toString() }.forEach { l ->
    if (l.language.isEmpty()) {
      return@forEach
    }
    val candidateFileName = "i18n_${l.language.lowercase()}_${l.country.uppercase()}.properties"
    if (translationFileNames.contains(candidateFileName)) {
      result.add(l)
    }
  }

  val locales = extraLocales.getProperty("_").split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
  for (l in locales) {
    if (!extraLocales.containsKey("$l.lang")) {
      continue
    }
    val langCode = extraLocales.getProperty("$l.lang")
    val countryCode = extraLocales.getProperty("$l.country", "")
    val regionCode = extraLocales.getProperty("$l.region", "")
    val locale = Locale(langCode, countryCode, regionCode)
    result.add(locale)
  }

  result.add(Locale.ENGLISH)

  val result1 = ArrayList(result)
  Collections.sort(result1, LEXICOGRAPHICAL_LOCALE_COMPARATOR)
  result1
}

private val extraLocales = Properties().also {
  PropertiesUtil.loadProperties(it, "/language/extra.properties")
}

private val LEXICOGRAPHICAL_LOCALE_COMPARATOR: Comparator<Locale> = Comparator { o1, o2 ->
  (o1.getDisplayLanguage(Locale.US) + o1.getDisplayCountry(Locale.US)).compareTo(
    o2.getDisplayLanguage(Locale.US) + o2.getDisplayCountry(Locale.US)
  )
}
