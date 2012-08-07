/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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
package net.sourceforge.ganttproject.document;

import javax.swing.Action;
import javax.swing.JComponent;

/**
 * Interface for the document selection UI.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public interface DocumentStorageUi {

  /**
   * Receives document descriptors as user changes selection in UI
   */
  public interface DocumentReceiver {
    void setDocument(Document document);
  }

  public class Components {
    public final JComponent contentPane;
    public final Action[] actions;

    public Components(JComponent contentPane, Action[] actions) {
      this.contentPane = contentPane;
      this.actions = actions;
    }
  }
  /**
   * Creates UI to open a document. UI should allow for selecting a document from a list or for typing
   * document access parameters. Every time user changes something, UI should send an updated
   * document descriptor to the receiver
   *
   * @param currentDocument document currently opened in GanttProject
   * @param receiver receiver of the descriptors of selected documents
   * @return UI component ready for inserting into a dialog or other component
   */
  Components open(Document currentDocument, DocumentReceiver receiver);

  /**
   * Creates UI to save a document. UI should allow for selecting a document from a list or for typing
   * document access parameters. Every time user changes something, UI should send an updated
   * document descriptor to the receiver
   *
   * @param currentDocument document currently opened in GanttProject
   * @param receiver receiver of the descriptors of selected documents
   * @return UI component ready for inserting into a dialog or other component
   */
  Components save(Document currentDocument, DocumentReceiver receiver);
}
