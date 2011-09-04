/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.time;

import java.util.Date;

/**
 * @author bard
 */
public class TimeUnitFunctionOfDateImpl extends TimeUnitDateFrameableImpl
        implements TimeUnitFunctionOfDate {
    private final DateFrameable myDirectFrameable;

    public TimeUnitFunctionOfDateImpl(String name, TimeUnitGraph graph,
            TimeUnit directAtomUnit, DateFrameable framer) {
        super(name, graph, directAtomUnit, framer);
        myDirectFrameable = directAtomUnit;
    }

    public TimeUnit createTimeUnit(Date date) {
        //TODO Only works if myBaseDate is not a TimeUnitFunctiongOfDateImpl! (Quarter -> Month -> Day fails!)
        return new ParameterizedTimeUnitImpl(date);
    }

    @Override
    public int getAtomCount(TimeUnit atomUnit) {
        throw new UnsupportedOperationException(
                "This time unit is function of date. Use method createTimeUnit() to create a parameterized instance.");
    }

    private class ParameterizedTimeUnitImpl implements TimeUnit, DateFrameable {
        private final Date myRightDate;

        private final Date myLeftDate;

        private int myAtomCount = -1;

        public ParameterizedTimeUnitImpl(Date myBaseDate) {
            this.myRightDate = TimeUnitFunctionOfDateImpl.this
                    .adjustRight(myBaseDate);
            this.myLeftDate = TimeUnitFunctionOfDateImpl.this
                    .adjustLeft(myBaseDate);
        }

        public String getName() {
            return TimeUnitFunctionOfDateImpl.this.getName();
        }

        public boolean isConstructedFrom(TimeUnit unit) {
            return TimeUnitFunctionOfDateImpl.this.isConstructedFrom(unit);
        }

        public int getAtomCount(TimeUnit atomUnit) {
            if (atomUnit == TimeUnitFunctionOfDateImpl.this || atomUnit == this) {
                return 1;
            }
            int directAtomCount = getDirectAtomCount();
            return directAtomCount * getDirectAtomUnit().getAtomCount(atomUnit);
        }

        private int getDirectAtomCount() {
            if (myAtomCount == -1) {
                myAtomCount = 0;
                for (Date leftDate = TimeUnitFunctionOfDateImpl.this.myDirectFrameable
                        .jumpLeft(myRightDate); leftDate.compareTo(myLeftDate) >= 0; myAtomCount++) {

                    leftDate = TimeUnitFunctionOfDateImpl.this.myDirectFrameable
                            .jumpLeft(leftDate);
                }
            }
            return myAtomCount;
        }

        public TimeUnit getDirectAtomUnit() {
            return TimeUnitFunctionOfDateImpl.this.getDirectAtomUnit();
        }

        public Date adjustRight(Date baseDate) {
            return TimeUnitFunctionOfDateImpl.this.adjustRight(baseDate);
        }

        public Date adjustLeft(Date baseDate) {
            return TimeUnitFunctionOfDateImpl.this.adjustLeft(baseDate);
        }

        public Date jumpLeft(Date baseDate) {
            return TimeUnitFunctionOfDateImpl.this.jumpLeft(baseDate);
        }

        public void setTextFormatter(TextFormatter formatter) {
            TimeUnitFunctionOfDateImpl.this.setTextFormatter(formatter);
        }

        public TimeUnitText format(Date baseDate) {
            TextFormatter formatter = TimeUnitFunctionOfDateImpl.this
                    .getTextFormatter();
            return formatter == null ? new TimeUnitText("") : formatter.format(
                    this, baseDate);
        }

        @Override
        public boolean equals(Object o) {
            return TimeUnitFunctionOfDateImpl.this.equals(o);
        }
    }
}
