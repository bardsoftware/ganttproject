/*
 LICENSE:
 
 This program is free software; you can redistribute it and/or modify  
 it under the terms of the GNU General Public License as published by  
 the Free Software Foundation; either version 2 of the License, or     
 (at your option) any later version.                                   
 
 Copyright (C) 2004, GanttProject Development Team
 */
package net.sourceforge.ganttproject.time;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard
 */
public class TimeUnitDateFrameableImpl extends TimeUnitImpl implements
        TimeUnit, DateFrameable {
    private final DateFrameable myFramer;

    public TimeUnitDateFrameableImpl(String name, TimeUnitGraph timeUnitGraph,
            TimeUnit atomUnit, DateFrameable framer) {
        super(name, timeUnitGraph, atomUnit);
        myFramer = framer;
    }

    public Date adjustRight(Date baseDate) {
        return myFramer.adjustRight(baseDate);
    }

    public Date adjustLeft(Date baseDate) {
        return myFramer.adjustLeft(baseDate);
    }

    public Date jumpLeft(Date baseDate) {
        return myFramer.jumpLeft(baseDate);
    }

}
