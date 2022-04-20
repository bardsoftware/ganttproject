package cloud.ganttproject.colloboque

import kotlinx.coroutines.channels.Channel
import javax.sql.DataSource

class ColloboqueServer(
  private val dataSourceFactory: (projectRefid: String) -> DataSource,
  private val initInputChannel: Channel<InitRecord>,
  private val updateInputChannel: Channel<ClientXlog>) {

}