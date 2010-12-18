/*
 * file:       GanttMSPDIOpen.java
 * author:     Jon Iles
 * copyright:  (c) Tapster Rock Limited 2005
 * date:       10/01/2005
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package org.ganttproject.impex.msproject;

import java.io.File;
import java.io.InputStream;

import net.sourceforge.ganttproject.IGanttProject;

import com.tapsterrock.mpx.MPXException;
import com.tapsterrock.mspdi.MSPDIFile;

/**
 * This class implements the mechanism used to import Microsoft Project data
 * from an MSPDI file into the GanttProject application.
 */
public class GanttMSPDIOpen extends GanttMPXJOpen {
    public GanttMSPDIOpen(IGanttProject project) {
        super(project);
    }

    public boolean load(File file) throws MPXException {
        return (load(new MSPDIFile(file)));
    }

    public boolean load(InputStream is) throws MPXException {
        return (load(new MSPDIFile(is)));
    }
}
