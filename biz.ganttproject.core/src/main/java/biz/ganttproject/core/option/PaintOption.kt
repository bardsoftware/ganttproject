/*
Copyright 2003-2026 Dmitry Barashev, GanttProject Team

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
package biz.ganttproject.core.option

import java.awt.Paint

/**
 * A colour option which is additionally able to describe itself with a full [java.awt.Paint],
 * such as a texture or a gradient.
 *
 * A [Paint] cannot be smuggled through [ColorOption] by subclassing [java.awt.Color]: the Java2D
 * pipeline short-circuits everything which is `instanceof Color` and never asks it for a paint
 * context, so the override is discarded without a warning. An option which wants to be drawn with
 * something else than a flat fill therefore has to say so explicitly, which is what this interface
 * is for.
 *
 * [value] stays the flat colour which approximates the paint, so that every consumer which only
 * understands a [java.awt.Color] keeps working unchanged. A UI which knows about this interface
 * may use [paint] instead; when [paint] is `null` it falls back to [value].
 */
interface PaintOption : ColorOption {
  val paint: Paint?
}
