package cloud.ganttproject.colloboque

import kotlinx.coroutines.channels.Channel

class ColloboqueServer(
  private val initInputChannel: Channel<InitRecord>,
  private val updateInputChannel: Channel<ClientXlog>) {

}