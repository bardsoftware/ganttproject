package net.sourceforge.ganttproject.storage

import org.h2.jdbcx.JdbcDataSource

interface ProjectStateStorageFactory {
  fun getStorage(): ProjectStateStorage
}

class H2InMemoryStorageFactory : ProjectStateStorageFactory {
  private companion object {
    const val H2_IN_MEMORY_URL = "jdbc:h2:mem:gantt-project-state"
  }

  override fun getStorage(): ProjectStateStorage {
    val dataSource = JdbcDataSource()
    dataSource.setURL(H2_IN_MEMORY_URL)
    return object : SqlStateStorageImpl(dataSource) {
      override fun shutdown() {
        try {
          this.dataSource.connection.use { conn ->
            conn.createStatement().execute("SHUTDOWN")
          }
        } catch (e: Exception) {
          // Ignore for now
        }
      }
    }
  }
}