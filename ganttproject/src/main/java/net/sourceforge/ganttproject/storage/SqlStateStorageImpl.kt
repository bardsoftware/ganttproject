package net.sourceforge.ganttproject.storage

import javax.sql.DataSource

open class SqlStateStorageImpl(protected val dataSource: DataSource) : ProjectStateStorage {
  override fun shutdown() {}
}