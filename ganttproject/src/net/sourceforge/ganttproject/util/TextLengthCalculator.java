/* LICENSE: GPL2
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.util;

/**
 * Contains methods to calculate the text length
 */
public interface TextLengthCalculator {

    /** @return the length of the text */
    int getTextLength(String text);

    /** @return the height of the text */
    int getTextHeight(String text);

    /**
     * @return the state of the object. Depending on internal settings a unique
     *         state is returned
     */
    Object getState();
}
