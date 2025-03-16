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

import biz.ganttproject.FXUtil
import biz.ganttproject.core.option.validatorI18N
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import java.text.MessageFormat
import java.text.NumberFormat
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
    val observable: SimpleStringProperty = SimpleStringProperty(),
    private var args: List<Any> = emptyList()) : ObservableValue<String> by observable {
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

  fun update(vararg args: Any): LocalizedString {
    this.args = args.toList()
    observable.value = build()
    return this
  }

  internal fun update() {
    observable.value = build()
  }

  private fun build(): String = i18n.formatText(key, *(args.toTypedArray()))
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

/**
 * This is a dummy localizer which can be used as a stub.
 */
object DummyLocalizer : Localizer {
  override fun create(key: String): LocalizedString {
    return LocalizedString(key, this)
  }

  override fun formatTextOrNull(key: String, vararg args: Any): String? {
    return null
  }
}

val DEFAULT_TRANSLATION_LOCALIZER = SingleTranslationLocalizer(defaultTranslation)
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
    private val baseLocalizer: Localizer = DEFAULT_TRANSLATION_LOCALIZER,
    private val prefixedLocalizer: Localizer? = null,
    private val currentTranslation: SimpleObjectProperty<Translation?> = SimpleObjectProperty(null)) : Localizer {

  override fun create(key: String): LocalizedString {
    return LocalizedString(key, this).also {
      currentTranslation.addListener { _, oldValue, newValue ->
        FXUtil.runLater {
          if (oldValue?.locale != newValue?.locale) {
            it.update()
          }
        }
      }
    }
  }

  override fun formatTextOrNull(key: String, vararg args: Any): String? {
    val prefixedKey = if (this.rootKey != "") "${this.rootKey}.$key" else key
    this.prefixedLocalizer?.formatTextOrNull(prefixedKey, *args)?.let {
      return it
    }
    return try {
      this.currentTranslation.value?.let { tr ->
        tr.mapKey(prefixedKey)?.let { value ->
          MessageFormat.format(value, *args)
        } ?: this.baseLocalizer.formatTextOrNull(key, *args)
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

class PrefixedLocalizer(
  private val prefix: String, private val base: Localizer, private val fallback: Localizer = DummyLocalizer
): Localizer {
  override fun create(key: String): LocalizedString = LocalizedString(key, this)

  override fun formatTextOrNull(key: String, vararg args: Any): String? {
    return this.base.formatTextOrNull("$prefix.$key", *args) ?: this.fallback.formatTextOrNull(key, *args)
  }
}

class IfNullLocalizer(private val base: Localizer, private val fallback: Localizer): Localizer {
  override fun create(key: String): LocalizedString = LocalizedString(key, this)

  override fun formatTextOrNull(key: String, vararg args: Any): String? {
    return this.base.formatTextOrNull(key, *args) ?: this.fallback.formatTextOrNull(key, *args)
  }
}
/**
 * This localizer searches for key values in a map. The map values are lambdas which
 * allows for values calculation.
 */
class MappingLocalizer(val key2lambda: Map<String, ()->LocalizedString?>, val unhandledKey: (String)->LocalizedString?) : Localizer {
  override fun create(key: String) = LocalizedString(key, this)

  override fun formatTextOrNull(key: String, vararg args: Any): String? =
    key2lambda[key]?.invoke()?.update(*args)?.value ?: unhandledKey(key)?.update(*args)?.value
}

/**
 * Localizer which always uses the given resource bundle.
 */
class SingleTranslationLocalizer(val bundle: Translation) : DefaultLocalizer(currentTranslation = SimpleObjectProperty(bundle), baseLocalizer = DummyLocalizer)


private var ourLocale: Locale = Locale.getDefault()
val ourCurrentTranslation: SimpleObjectProperty<Translation?> = SimpleObjectProperty(null)
var RootLocalizer : DefaultLocalizer = DefaultLocalizer(currentTranslation = ourCurrentTranslation).also {
  validatorI18N = it::formatText
}

fun createDefaultLocalizer(fallback: Localizer): DefaultLocalizer {
  return DefaultLocalizer(baseLocalizer = fallback, currentTranslation = ourCurrentTranslation)
}

fun setLocale(locale: Locale) {
  ourLocale = locale
  ourCurrentTranslation.value = createTranslation(locale) ?: defaultTranslation
}

fun String.removeMnemonicsPlaceholder(): String = this.replace("$", "")

fun getNumberFormat(): NumberFormat = NumberFormat.getInstance(ourLocale)

data class Translation(val locale: Locale, val mapKey: (String) -> String?)
