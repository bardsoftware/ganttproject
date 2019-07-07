/*
Copyright 2019 BarD Software s.r.o

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
package biz.ganttproject.app

import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.util.PropertiesUtil
import org.eclipse.core.runtime.Platform
import java.text.MessageFormat
import java.util.*

class LocalizedString(
    private val key: String,
    private val i18n: Localizer,
    private val observable: SimpleStringProperty = SimpleStringProperty(),
    private var args: List<String> = emptyList()) : ObservableValue<String> by observable {
  init {
    observable.value = build()
  }

  fun update(vararg args: String): LocalizedString {
    this.args = args.toList()
    observable.value = build()
    return this
  }

  private fun build(): String = i18n.formatText(key, *args.toTypedArray())
}

interface Localizer {
  fun create(key: String): LocalizedString
  fun formatText(key: String, vararg args: Any): String
  fun formatTextOrNull(key: String, vararg args: Any): String?
}

object DummyLocalizer : Localizer {
  override fun create(key: String): LocalizedString {
    return LocalizedString(key, this)
  }

  override fun formatText(key: String, vararg args: Any): String {
    return key
  }

  override fun formatTextOrNull(key: String, vararg args: Any): String? {
    return null
  }

}

/**
 * @author dbarashev@bardsoftware.com
 */
class DefaultLocalizer(var rootKey: String = "", private val fallbackLocalizer: Localizer = DummyLocalizer) : Localizer {
  override fun create(key: String): LocalizedString = LocalizedString(key, this)

  override fun formatText(key: String, vararg args: Any): String {
    return formatTextOrNull(key, *args) ?: key
  }

  override fun formatTextOrNull(key: String, vararg args: Any): String? {
    val key1 = if (this.rootKey != "") "${this.rootKey}.$key" else key
    return try {
      currentTranslation?.let { tr ->
        if (tr.containsKey(key1)) {
          MessageFormat.format(tr.getString(key1), *args)
        } else {
          this.fallbackLocalizer.formatTextOrNull(key, args)
        }
      }
    } catch (ex: MissingResourceException) {
      null
    }
  }

  fun hasKey(key: String): Boolean {
    return currentTranslation?.containsKey(key) ?: false
  }
}

val RootLocalizer = DefaultLocalizer()

private var currentTranslation: ResourceBundle? = getResourceBundle(Locale.getDefault(), true)
fun setLocale(locale: Locale) {
  currentTranslation = getResourceBundle(locale, true)
  println("Current translation =${currentTranslation?.locale}")
}

private fun getResourceBundle(locale: Locale, withFallback: Boolean): ResourceBundle? {
  return Platform.getExtensionRegistry().getConfigurationElementsFor("net.sourceforge.ganttproject.l10n")
      .mapNotNull { l10nConfig ->
        val path = l10nConfig.getAttribute("path")
        val pluginBundle = Platform.getBundle(l10nConfig.declaringExtension.namespaceIdentifier)
            ?: error("Can't find plugin bundle for extension=" + l10nConfig.name)
        try {
          val control = if (withFallback)
            ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES)
          else
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)
          val resourceBundle = ResourceBundle.getBundle(path, locale, pluginBundle.bundleClassLoader, control)
          if (withFallback || resourceBundle.locale == locale) {
            resourceBundle
          } else {
            null
          }
        } catch (ex: MissingResourceException) {
          GPLogger.logToLogger(String.format("Can't find bundle: path=%s locale=%s plugin bundle=%s", path, locale, pluginBundle))
          null
        }
      }
      .firstOrNull()
}

private val extraLocales = Properties().also {
  PropertiesUtil.loadProperties(it, "/language/extra.properties")
}

private val LEXICOGRAPHICAL_LOCALE_COMPARATOR: Comparator<Locale> = Comparator { o1, o2 ->
  (o1.getDisplayLanguage(Locale.US) + o1.getDisplayCountry(Locale.US)).compareTo(
      o2.getDisplayLanguage(Locale.US) + o2.getDisplayCountry(Locale.US)
  )
}

fun getAvailableTranslations(): List<Locale> {
  val removeLangOnly = HashSet<Locale>()
  val result = HashSet<Locale>()
  for (l in Locale.getAvailableLocales()) {
    if (l.language.isEmpty()) {
      continue
    }
    if (getResourceBundle(l, false) != null) {
      if (l.country.isNotEmpty()) {
        removeLangOnly.add(Locale(l.language))
      }
      result.add(Locale(l.language, l.country))
    } else {
      val langOnly = Locale(l.language)
      if (getResourceBundle(langOnly, false) != null) {
        result.add(langOnly)
      }
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

  result.removeAll(removeLangOnly)
  result.add(Locale.ENGLISH)

  val result1 = ArrayList(result)
  Collections.sort(result1, LEXICOGRAPHICAL_LOCALE_COMPARATOR)
  return result1
}

