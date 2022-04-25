/*
Copyright 2022 BarD Software s.r.o., GanttProject Cloud OU

This file is part of GanttProject Cloud.

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
package cloud.ganttproject.colloboque

import kotlinx.coroutines.channels.Channel
import javax.sql.DataSource
import net.sourceforge.ganttproject.storage.InitRecord
import net.sourceforge.ganttproject.storage.InputXlog

class ColloboqueServer(
  private val dataSourceFactory: (projectRefid: String) -> DataSource,
  private val initInputChannel: Channel<InitRecord>,
  private val updateInputChannel: Channel<InputXlog>) {

  fun init(projectRefid: String) {
    dataSourceFactory(projectRefid).connection.use {
      it.createStatement().executeQuery("SELECT uid FROM Task").use { rs ->
        while (rs.next()) {
          println(rs.getString(1))
        }
      }
    }
  }

}