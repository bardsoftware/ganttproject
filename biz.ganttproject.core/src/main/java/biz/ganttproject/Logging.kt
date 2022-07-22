/*
Copyright 2020 BarD Software s.r.o

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
package biz.ganttproject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * @author dbarashev@bardsoftware.com
 */
interface LoggerApi<T> {
  fun error(msg: String, vararg params: Any, kv: Map<String, Any> = emptyMap(), exception: Throwable? = null)
  fun debug(msg: String, vararg params: Any?, kv: Map<String, Any> = emptyMap())
  fun delegate(): T
  fun debug(msg: String) {
    debug(msg, params = arrayOf())
  }
}

class LoggerImpl(name: String) : LoggerApi<Logger> {
  private val delegate = LoggerFactory.getLogger(name)

  override fun error(msg: String, vararg params: Any, kv: Map<String, Any>, exception: Throwable?) {
    kv.mapValues { it.value.toString() }
        .forEach { if (it.value.isNotBlank()) MDC.put(it.key, it.value) }
    delegate.error(msg, *params, exception)
    MDC.clear()
  }

  override fun debug(msg: String, vararg params: Any?, kv: Map<String, Any>) {
    kv.mapValues { it.value.toString() }
        .forEach { if (it.value.isNotBlank()) MDC.put(it.key, it.value) }
    delegate.debug(msg, *params)
    MDC.clear()
  }

  override fun delegate() = delegate

}
