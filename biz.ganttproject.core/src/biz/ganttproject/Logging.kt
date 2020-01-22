package biz.ganttproject

import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * @author dbarashev@bardsoftware.com
 */
interface LoggerApi {
  fun debug(msg: String, vararg params: Any, kv: Map<String, Any> = emptyMap())
}

class LoggerImpl(private val name: String) : LoggerApi {
  private val delegate = LoggerFactory.getLogger(name)

  override fun debug(msg: String, vararg params: Any, kv: Map<String, Any>) {
    kv.mapValues { it.value?.toString() }.filterValues { it != null }
        .forEach { if (it.value.isNotBlank()) MDC.put(it.key, it.value) }
    delegate.debug(msg, params)
    MDC.clear()
  }

}
