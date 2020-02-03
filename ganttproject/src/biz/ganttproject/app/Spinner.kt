/*
Copyright 2020 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

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
package biz.ganttproject.app

import biz.ganttproject.FXUtil
import biz.ganttproject.storage.cloud.GPCloudStorage
import javafx.animation.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.util.Duration

/**
 * This is a progress indicator which show a gray-shaded bee icon when spinning and colored
 * jumping bee icon when progress is completed and user attention is required.
 *
 * @author dbarashev@bardsoftware.com
 */
class Spinner(initialState: State = State.INITIAL) {
  enum class State {
    INITIAL, WAITING, ATTENTION
  }
  enum class IconType {
    BEE_BW, CIRCLE_COLOR
  }
  private var iconView = createIconView(IconType.BEE_BW)
  private val iconPane = BorderPane().apply {
    center = iconView
  }
  private var animationStopper: (()->Unit) ? = null

  val pane: Region get() = iconPane
  var state: State = State.INITIAL
  set(value) {
    animationStopper?.invoke()
    when (value) {
      State.WAITING -> animationStopper = iconView.rotate()
      State.ATTENTION -> {
        iconView = createIconView(IconType.CIRCLE_COLOR)
        FXUtil.transitionCenterPane(iconPane, iconView) {}
        animationStopper = iconView.jump()
      }
      State.INITIAL -> {
        iconView = createIconView(IconType.BEE_BW)
        FXUtil.transitionCenterPane(iconPane, iconView) {}
      }
    }
    field = value
  }

  init {
    state = initialState
  }

  private fun createIconView(type: IconType) =
    ImageView(Image(
        GPCloudStorage::class.java.getResourceAsStream(
            when (type) {
              IconType.BEE_BW -> "/icons/ganttproject-logo-bee-bw-512.png"
              IconType.CIRCLE_COLOR -> "/icons/ganttproject-logo-512.png"
            }
        ),
        128.0, 128.0, false, true))

}

private fun (ImageView).rotate() : ()->Unit {
  val rt = RotateTransition(Duration.millis(3000.0), this)
  rt.fromAngle = 0.0
  rt.toAngle = 360.0
  rt.cycleCount = Animation.INDEFINITE
  rt.interpolator = Interpolator.LINEAR
  rt.play()

  return rt::stop
//  return {
//    rt.cycleCount = 1
//    rt.duration = Duration.millis(1000.0)
//    rt.playFromStart()
//  }
}

private fun (ImageView).jump() : ()->Unit {
  val longJump = TranslateTransition(Duration.millis(200.0), this)
  longJump.interpolatorProperty().set(Interpolator.SPLINE(.1, .1, .7, .7))
  longJump.byY = -30.0
  longJump.isAutoReverse = true
  longJump.cycleCount = 2

  val shortJump = TranslateTransition(Duration.millis(100.0), this)
  shortJump.interpolatorProperty().set(Interpolator.SPLINE(.1, .1, .7, .7))
  shortJump.byY = -15.0
  shortJump.isAutoReverse = true
  shortJump.cycleCount = 6

  val pause = PauseTransition(Duration.seconds(2.0))
  val seq = SequentialTransition(longJump, shortJump, pause)
  seq.cycleCount = Animation.INDEFINITE
  seq.interpolator = Interpolator.LINEAR
  seq.play()
  return seq::stop
}

