package biz.ganttproject.app

import org.slf4j.LoggerFactory

typealias LocalizerBuilderFn = LocalizerBuilder.()->Unit

class LocalizerBuilder {
  var currentLocalizer: Localizer = DummyLocalizer

  fun build() = currentLocalizer

  fun default(withFallback: Boolean = true) {
    currentLocalizer = if (withFallback) {
      RootLocalizer
    } else {
      SingleTranslationLocalizer(ourCurrentTranslation.value!!)
    }
  }

  fun prefix(prefix: String, fallbackBuilder: LocalizerBuilderFn? = null) {
    val fallback = fallbackBuilder?.let { LocalizerBuilder().let {
      it.fallbackBuilder()
      it.build()
    }} ?: DummyLocalizer
    currentLocalizer = PrefixedLocalizer(prefix, base = currentLocalizer, fallback = fallback)
  }

  fun map(keyMapping: Map<String, String>, fallbackBuilder: LocalizerBuilderFn? = null) {
    val fallback = fallbackBuilder?.let { LocalizerBuilder().let {
      it.fallbackBuilder()
      it.build()
    }} ?: DummyLocalizer
    val base = currentLocalizer
    val mappingLocalizer = MappingLocalizer(keyMapping.mapValues { entry -> { base.create(entry.value) } },
      unhandledKey = {null}
    )
    currentLocalizer = IfNullLocalizer(mappingLocalizer, fallback)
  }

  fun transform(fn: (String) -> String) {
    currentLocalizer = KeyTransformLocalizer(fn, currentLocalizer)
  }

  fun fallback(fallbackBuilder: LocalizerBuilderFn? = null) {
    val base = currentLocalizer
    val fallback = fallbackBuilder?.let { LocalizerBuilder().let {
      it.fallbackBuilder()
      it.build()
    }} ?: DummyLocalizer
    currentLocalizer = IfNullLocalizer(base, fallback)
  }

  fun debug(id: String) {
    currentLocalizer = DebuggingLocalizer(id, currentLocalizer)
  }

}

class KeyTransformLocalizer(private val fn: (String) -> String, private val delegate: Localizer): Localizer {
  override fun create(key: String): LocalizedString = LocalizedString(key, this)

  override fun formatTextOrNull(key: String, vararg args: Any): String? {
    return delegate.formatTextOrNull(fn(key), args)
  }

}
class DebuggingLocalizer(private val id: String, private val delegate: Localizer): Localizer {
  override fun create(key: String): LocalizedString = LocalizedString(key, this)

  override fun formatTextOrNull(key: String, vararg args: Any): String? {
    LOG.info("Searching for key={} in localizer={}", key, id)
    println("Searching for key=${key} in localizer=${id}")
    return delegate.formatTextOrNull(key, args).also {
      println(".... result: $it")
    }
  }
}

private val LOG = LoggerFactory.getLogger("I18N")
fun i18n(fn: LocalizerBuilderFn) =
  LocalizerBuilder().let {
    it.fn()
    it.build()
  }



fun foo() {
  i18n {
    default()

  }
}