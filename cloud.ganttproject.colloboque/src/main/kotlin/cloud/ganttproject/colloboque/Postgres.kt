package cloud.ganttproject.colloboque

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class PostgresDataSourceFactory(
  private val pgHost: String, private val pgPort: Int, private val pgSuperUser: String, private val pgSuperAuth: String
) {
  private val superConfig = HikariConfig().apply {
    username = pgSuperUser
    password = pgSuperAuth
    jdbcUrl = "jdbc:postgresql://${pgHost}:${pgPort}/colloboque"
  }
  private val superDataSource = HikariDataSource(superConfig)

  init {
    superDataSource.connection.use {
      it.createStatement().executeQuery("SELECT version()").use { rs ->
        if (rs.next()) {
          LoggerFactory.getLogger("Startup").info("Connected to the master database. {}", rs.getString(1))
        }
      }
    }
  }

  fun createDataSource(projectRefid: String): DataSource {
    TODO("Write your code here")
  }
}