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

/**
 * Localized string is an observable localized string with parameters.
 * The typical use case is:
 * 1. Client code creates string from key, e.g. "hello", and passes argument "World"
 * 2. Internationalization code here searches for "hello" key in the localizer (it is usually a resource bundle)
 *    and finds e.g. "Hello {0}" pattern
 * 3. Pattern is applied to the arguments and we get "Hello World" which becomes a new value of observable
 * 4. Client code then updates the arguments and passes "GanttProject". The process repeats and new observable
 *    value "Hello GanttProject" is submitted.
 *
 * Normally instances are created with a factory in Localizer.
 */
class LocalizedString(
    private val key: String,
    private val i18n: Localizer,
    private val observable: SimpleStringProperty = SimpleStringProperty(),
    private var args: List<String> = emptyList()) : ObservableValue<String> by observable {
  init {
    observable.value = build()
  }

  fun clear() {
    observable.value = ""
  }

  fun update(vararg args: String): LocalizedString {
    this.args = args.toList()
    observable.value = build()
    return this
  }

  private fun build(): String = i18n.formatText(key, *args.toTypedArray())
}

/**
 * Creates localized observable strings, formats messages with parameters and manages current translation.
 */
interface Localizer {
  /**
   * Creates a new localized string from the given message key.
   */
  fun create(key: String): LocalizedString

  /**
   * Applies pattern by the given key to the given arguments. By default, it calls formatTextOrNull
   * and returns key itself if the latter returns null
   */
  fun formatText(key: String, vararg args: Any): String {
    return formatTextOrNull(key, *args) ?: key
  }

  /**
   * Searches for message by the given key and applies it to the given arguments.
   * Returns null if message is not found.
   */
  fun formatTextOrNull(key: String, vararg args: Any): String?
}

object DummyLocalizer : Localizer {
  override fun create(key: String): LocalizedString {
    return LocalizedString(key, this)
  }

  override fun formatTextOrNull(key: String, vararg args: Any): String? {
    return null
  }
}

/**
 * This localizer allows for flexible use of shared resource bundles.
 * When searching for a message by the given message key, it first prepends the rootKey prefix to the
 * message key. If prefixed localizer is set, it is consulted first. This way we can just
 * use shorter message keys for a group of logically related keys (e.g. use root key "exitDialog" and
 * keys "title", "message", "ok" instead of "exitDialog.title", "exitDialog.message" and "exitDialog.ok").
 *
 * If root localizer is not set or returns no message, the message is searched by prefixed key in the local
 * resource bundle of this localizer. In case of success it is formatted with MessageFormat, otherwise
 * base localizer is consulted with original message key. This way we can use a pool of common messages
 * which is shared between more specific localizers. E.g., for a set of dialogs where submit and cancel
 * buttons are usually labeled with "OK" and "Cancel", we can create a shared base localizer L0 with keys
 * "ok" and "cancel". For a dialog which requests user to accept some terms, we can create a localizer L1
 * with root key "acceptTerms", key "acceptTerms.ok"="Accept" and L0 as a base localizer.
 *
 * When submit and cancel buttons in accept terms dialog are constructed, they will call localizer L1
 * and pass "ok" and "cancel" keys. L1 will find "acceptTerms.ok" in its own bundle and will pass "cancel"
 * to the base localizer.
 *
 * @author dbarashev@bardsoftware.com
 */
open class DefaultLocalizer(
    private val rootKey: String = "",
    private val baseLocalizer: Localizer = DummyLocalizer,
    private val prefixedLocalizer: Localizer? = null,
    private val currentTranslation: () -> ResourceBundle? = { null }) : Localizer {
  override fun create(key: String): LocalizedString {
    return LocalizedString(key, this)
  }

  override fun formatTextOrNull(key: String, vararg args: Any): String? {
    val prefixedKey = if (this.rootKey != "") "${this.rootKey}.$key" else key
    this.prefixedLocalizer?.formatTextOrNull(prefixedKey, *args)?.let {
      return it
    }
    return try {
      this.currentTranslation()?.let { tr ->
        if (tr.containsKey(prefixedKey)) {
          MessageFormat.format(tr.getString(prefixedKey), *args)
        } else {
          this.baseLocalizer.formatTextOrNull(key, *args)
        }
      }
    } catch (ex: MissingResourceException) {
      null
    }
  }

  /**
   * Creates a new localizer which uses this one as "prefixed" with the given prefix.
   */
  fun createWithRootKey(rootKey: String, baseLocalizer: Localizer = DummyLocalizer): DefaultLocalizer =
      DefaultLocalizer(rootKey, baseLocalizer, this, this.currentTranslation)
}

/**
 * Localizer which always uses the given resource bundle.
 */
class SingleTranslationLocalizer(val bundle: ResourceBundle) : DefaultLocalizer(currentTranslation = {bundle})

var RootLocalizer : DefaultLocalizer = DefaultLocalizer(currentTranslation = { ourCurrentTranslation })

private var ourCurrentTranslation: ResourceBundle? = getResourceBundle(Locale.getDefault(), true)
fun setLocale(locale: Locale) {
  ourCurrentTranslation = getResourceBundle(locale, true)
}

private fun getResourceBundle(locale: Locale, withFallback: Boolean): ResourceBundle? {
  return Platform.getExtensionRegistry()?.getConfigurationElementsFor("net.sourceforge.ganttproject.l10n")
      ?.mapNotNull { l10nConfig ->
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
      ?.firstOrNull()
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

