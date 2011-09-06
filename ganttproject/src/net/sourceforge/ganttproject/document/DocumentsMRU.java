/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 Michael Haeusler, GanttProject Team

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
package net.sourceforge.ganttproject.document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * List of Documents MRU (<b>M</b>ost <b>R</b>ecently <b>U</b>sed)
 *
 * @author Michael Haeusler (michael at akatose.de)
 */
public class DocumentsMRU {

    private int maxSize;

    private List<Document> documents;

    public DocumentsMRU(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be larger than zero (" + maxSize + ")");
        }
        this.maxSize = maxSize;
        documents = new ArrayList<Document>(maxSize);
    }

    /**
     * Adds a Document at the top of the list of Documents MRU. If the list is
     * already full, an old entry is removed, to make place for this new
     * document.
     *
     * @param document
     *            the Document that should be added
     * @return true when the list has changed through the addition
     */
    public boolean add(Document document) {
        // if the document is invalid, we don't add it
        if (!document.isValidForMRU())
            return false;

        int i = documents.indexOf(document);
        // if the document is already on the top,
        // nothing needs to change.
        if (0 == i)
            return false;

        if (0 < i) {
            documents.remove(i);
        } else {
            while (documents.size() >= maxSize) {
                documents.remove(maxSize - 1);
            }
        }
        documents.add(0, document);

        return true;
    }

    /** clears the list of Documents MRU */
    public void clear() {
        documents.clear();
    }

    /** @return an Iterator over the entries of the list of Documents MRU */
    public Iterator<Document> iterator() {
        return documents.iterator();
    }

}
