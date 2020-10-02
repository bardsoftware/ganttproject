/*
Copyright 2020 Dmitry Kazakov, BarD Software s.r.o

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
package biz.ganttproject.core.chart.scene

import biz.ganttproject.core.chart.scene.CapacityHeatmapSceneBuilder.Load
import junit.framework.TestCase
import org.junit.jupiter.api.Assertions.assertIterableEquals
import java.text.SimpleDateFormat

class CapacityHeatmapSceneBuilderTest : TestCase() {
  private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
  private val initialBorder = LoadBorder(Long.MIN_VALUE, 0f)

  fun `test zero loads are not counted`() {
    val loads = listOf(
        Load("2020-09-1".toTs(), "2020-09-11".toTs(), 0f),
        Load("2020-09-5".toTs(), "2020-09-6".toTs(), 1f),
        Load("2020-09-3".toTs(), "2020-09-11".toTs(), 0f)
    )
    val expected = listOf(
        initialBorder, LoadBorder("2020-09-5".toTs(), 1f), LoadBorder("2020-09-6".toTs(), 0f)
    )
    assertIterableEquals(expected, calcLoadDistribution(loads))
  }


  fun `test nested loads`() {
    val loads = listOf(
        Load("2020-09-15".toTs(), "2020-09-17".toTs(), 4f),
        Load("2020-09-1".toTs(), "2020-09-20".toTs(), 1f),
        Load("2020-09-2".toTs(), "2020-09-7".toTs(), 2f),
        Load("2020-09-3".toTs(), "2020-09-4".toTs(), 3f)
    )
    val expected = listOf(
        initialBorder, LoadBorder("2020-09-1".toTs(), 1f), LoadBorder("2020-09-2".toTs(), 3f),
        LoadBorder("2020-09-3".toTs(), 6f), LoadBorder("2020-09-4".toTs(), 3f), LoadBorder("2020-09-7".toTs(), 1f),
        LoadBorder("2020-09-15".toTs(), 5f), LoadBorder("2020-09-17".toTs(), 1f), LoadBorder("2020-09-20".toTs(), 0f)
    )
    assertIterableEquals(expected, calcLoadDistribution(loads))
  }

  fun `test loads intersection`() {
    val loads = listOf(
        Load("2020-09-10".toTs(), "2020-09-20".toTs(), 1f),
        Load("2020-09-5".toTs(), "2020-09-15".toTs(), 2f),
        Load("2020-09-8".toTs(), "2020-09-12".toTs(), 3f)
    )
    val expected = listOf(
        initialBorder, LoadBorder("2020-09-5".toTs(), 2f), LoadBorder("2020-09-8".toTs(), 5f),
        LoadBorder("2020-09-10".toTs(), 6f), LoadBorder("2020-09-12".toTs(), 3f), LoadBorder("2020-09-15".toTs(), 1f),
        LoadBorder("2020-09-20".toTs(), 0f)
    )
    assertIterableEquals(expected, calcLoadDistribution(loads))
  }

  fun `test loads with matching borders`() {
    val loads = listOf(
        Load("2020-09-1".toTs(), "2020-09-20".toTs(), 1f),
        Load("2020-09-1".toTs(), "2020-09-3".toTs(), 2f),
        Load("2020-09-3".toTs(), "2020-09-10".toTs(), 3f),
        Load("2020-09-1".toTs(), "2020-09-20".toTs(), 4f)
    )
    val expected = listOf(
        initialBorder, LoadBorder("2020-09-1".toTs(), 7f), LoadBorder("2020-09-3".toTs(), 8f),
        LoadBorder("2020-09-10".toTs(), 5f), LoadBorder("2020-09-20".toTs(), 0f)
    )
    assertIterableEquals(expected, calcLoadDistribution(loads))
  }

  private fun String.toTs() = dateFormat.parse(this).time
}
