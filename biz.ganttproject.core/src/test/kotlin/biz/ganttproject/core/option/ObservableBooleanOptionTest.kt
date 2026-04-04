/*
Copyright 2026 Dmitry Barashev, BarD Software s.r.o

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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ObservableBooleanOptionTest {

    @Test
    fun testDelegation() {
        val obsBoolean = ObservableBoolean("test.id", false)
        val option = ObservableBooleanOption(obsBoolean)

        assertEquals("test.id", option.id)
        assertFalse(option.value)
        assertFalse(option.isChecked)

        option.value = true
        assertTrue(obsBoolean.value)
        assertTrue(option.value)
        assertTrue(option.isChecked)

        obsBoolean.value = false
        assertFalse(option.value)
        assertFalse(option.isChecked)
    }

    @Test
    fun testToggle() {
        val obsBoolean = ObservableBoolean("test.id", false)
        val option = ObservableBooleanOption(obsBoolean)

        option.toggle()
        assertTrue(obsBoolean.value)
        assertTrue(option.value)

        option.toggle()
        assertFalse(obsBoolean.value)
        assertFalse(option.value)
    }

    @Test
    fun testChangeListeners() {
        val obsBoolean = ObservableBoolean("test.id", false)
        val option = ObservableBooleanOption(obsBoolean)
        var changeCount = 0
        option.addChangeValueListener {
            changeCount++
            assertFalse(it.oldValue as Boolean)
            assertTrue(it.newValue as Boolean)
        }

        obsBoolean.value = true
        assertEquals(1, changeCount)
    }

    @Test
    fun testIsWritableDelegation() {
        val obsBoolean = ObservableBoolean("test.id", false)
        val option = ObservableBooleanOption(obsBoolean)

        assertTrue(option.isWritable)
        obsBoolean.setWritable(false)
        assertFalse(option.isWritable)
        
        obsBoolean.setWritable(true)
        assertTrue(option.isWritable)
    }
}
