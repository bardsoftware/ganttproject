/**
 * Copyright (c) 2015, 2016 ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package biz.ganttproject.lib.fx

import javafx.animation.TranslateTransition
import javafx.beans.property.DoubleProperty
import javafx.css.CssMetaData
import javafx.css.Styleable
import javafx.css.StyleableDoubleProperty
import javafx.css.StyleableProperty
import javafx.css.converter.SizeConverter
import javafx.event.EventHandler
import javafx.scene.control.Skin
import javafx.scene.control.SkinBase
import javafx.scene.layout.StackPane
import javafx.util.Duration
import org.controlsfx.control.ToggleSwitch
import java.util.*

/**
 * Basic Skin implementation for the [ToggleSwitch]
 */
class ToggleSwitchSkin(control: ToggleSwitch) : SkinBase<ToggleSwitch>(control) {
  private val thumb: StackPane
  private val thumbArea: StackPane
  private val transition: TranslateTransition?


  /**
   * How many milliseconds it should take for the thumb to go from
   * one edge to the other
   */
  private val thumbMoveAnimationTime: DoubleProperty = object : StyleableDoubleProperty(200.0) {

    override fun getBean(): Any {
      return this@ToggleSwitchSkin
    }

    override fun getName(): String {
      return "thumbMoveAnimationTime"
    }

    override fun getCssMetaData(): CssMetaData<ToggleSwitch, Number> {
      return THUMB_MOVE_ANIMATION_TIME
    }
  }

  init {
    thumb = StackPane()
    thumbArea = StackPane()
    transition = TranslateTransition(Duration.millis(getThumbMoveAnimationTime()), thumb)

    children.addAll(thumbArea, thumb)

    thumb.styleClass.setAll("thumb")
    thumbArea.styleClass.setAll("thumb-area")

    thumbArea.onMouseReleased = EventHandler { event -> mousePressedOnToggleSwitch(control) }
    thumb.onMouseReleased = EventHandler { event -> mousePressedOnToggleSwitch(control) }
    control.selectedProperty().addListener { observable, oldValue: Boolean, newValue ->
      if (newValue!! != oldValue!!)
        selectedStateChanged()
    }
  }

  private fun selectedStateChanged() {
    transition?.stop()

    val thumbAreaWidth = snapSize(thumbArea.prefWidth(-1.0))
    val thumbWidth = snapSize(thumb.prefWidth(-1.0))
    val distance = thumbAreaWidth - thumbWidth
    /**
     * If we are not selected, we need to go from right to left.
     */
    if (!skinnable.isSelected) {
      thumb.layoutX = thumbArea.layoutX
      transition!!.fromX = distance
      transition.toX = 0.0
    } else {
      thumb.translateX = thumbArea.layoutX
      transition!!.fromX = 0.0
      transition.toX = distance
    }
    transition.cycleCount = 1
    transition.play()
  }

  private fun mousePressedOnToggleSwitch(toggleSwitch: ToggleSwitch) {
    toggleSwitch.isSelected = !toggleSwitch.isSelected
  }

  private fun thumbMoveAnimationTimeProperty(): DoubleProperty {
    return thumbMoveAnimationTime
  }

  private fun getThumbMoveAnimationTime(): Double {
    return if (thumbMoveAnimationTime == null) 200.0 else thumbMoveAnimationTime!!.get()
  }

  override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
    val toggleSwitch = skinnable
    val thumbWidth = snapSize(thumb.prefWidth(-1.0))
    val thumbHeight = snapSize(thumb.prefHeight(-1.0))
    thumb.resize(thumbWidth, thumbHeight)
    //We must reset the TranslateX otherwise the thumb is mis-aligned when window is resized.
    transition?.stop()
    thumb.translateX = 0.0

    val thumbAreaY = snapPosition(contentY)
    val thumbAreaWidth = snapSize(thumbArea.prefWidth(-1.0))
    val thumbAreaHeight = snapSize(thumbArea.prefHeight(-1.0))

    thumbArea.resize(thumbAreaWidth, thumbAreaHeight)
    thumbArea.layoutX = 0.0
    thumbArea.layoutY = thumbAreaY

    if (!toggleSwitch.isSelected)
      thumb.layoutX = thumbArea.layoutX
    else
      thumb.layoutX = thumbArea.layoutX + thumbAreaWidth - thumbWidth
    thumb.layoutY = thumbAreaY + (thumbAreaHeight - thumbHeight) / 2
  }


  override fun computeMinWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
    return leftInset + thumbArea.prefWidth(-1.0) + rightInset
  }

  override fun computeMinHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
    return topInset + Math.max(thumb.prefHeight(-1.0), 0.0) + bottomInset
  }

  override fun computePrefWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
    return leftInset  + 20.0 + thumbArea.prefWidth(-1.0) + rightInset
  }

  override fun computePrefHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
    return topInset + Math.max(thumb.prefHeight(-1.0), 0.0) + bottomInset
  }

  override fun computeMaxWidth(height: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
    return skinnable.prefWidth(height)
  }

  override fun computeMaxHeight(width: Double, topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
    return skinnable.prefHeight(width)
  }

  /**
   * {@inheritDoc}
   */
  override fun getCssMetaData(): List<CssMetaData<out Styleable, *>> {
    return classCssMetaData
  }

  companion object {

    private val THUMB_MOVE_ANIMATION_TIME = object : CssMetaData<ToggleSwitch, Number>("-thumb-move-animation-time",
        SizeConverter.getInstance(), 200) {

      override fun isSettable(toggleSwitch: ToggleSwitch): Boolean {
        val skin = toggleSwitch.skin as ToggleSwitchSkin
        return skin.thumbMoveAnimationTime == null || !skin.thumbMoveAnimationTime!!.isBound
      }

      override fun getStyleableProperty(toggleSwitch: ToggleSwitch): StyleableProperty<Number> {
        val skin = toggleSwitch.skin as ToggleSwitchSkin
        return skin.thumbMoveAnimationTimeProperty() as StyleableProperty<Number>
      }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    val classCssMetaData: List<CssMetaData<out Styleable, *>>

    init {
      val styleables = ArrayList(SkinBase.getClassCssMetaData())
      styleables.add(THUMB_MOVE_ANIMATION_TIME)
      classCssMetaData = Collections.unmodifiableList(styleables)
    }
  }
}

fun createToggleSwitch(): ToggleSwitch = object : ToggleSwitch() {
  override fun createDefaultSkin(): Skin<*> {
    return ToggleSwitchSkin(this)
  }
}
