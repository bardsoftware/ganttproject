/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2003-2011 Dmitry Barashev, GanttProject Team

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

package net.sourceforge.ganttproject.document.webdav;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import net.sourceforge.ganttproject.document.DocumentStorageUi.DocumentDescriptor;
import net.sourceforge.ganttproject.document.DocumentStorageUi.DocumentReceiver;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.SpringUtilities;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.IntegerOption;
import net.sourceforge.ganttproject.gui.options.model.StringOption;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Dialog for Open/Save file from/to WebDAV resource.
 *
 * @author Dmitry Barashev (major rewrite).
 * @author Alexandre Thomas (initial version).
 */
class GanttURLChooser {
  private static final GanttLanguage language = GanttLanguage.getInstance();

  private final StringOption myUrl;

  private final StringOption myUsername;

  private final StringOption myPassword;

  private final BooleanOption myLock;

  private final IntegerOption myTimeout;


  public GanttURLChooser(String url, String username, String password, IntegerOption lockTimeoutOption, final DocumentReceiver receiver) {
    myUrl = new DefaultStringOption("url", url);
    myUsername = new DefaultStringOption("username", username);
    myPassword = new DefaultStringOption("password", password);
    myLock = new DefaultBooleanOption("lock", true);
    myTimeout = lockTimeoutOption;

    ChangeValueListener listener = new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        receiver.setDocument(createDocument());
      }
    };
    myUrl.addChangeValueListener(listener);
    myUsername.addChangeValueListener(listener);
    myPassword.addChangeValueListener(listener);
  }

  protected DocumentDescriptor createDocument() {
    return new DocumentDescriptor(myUrl.getValue(), myUsername.getValue(), myPassword.getValue());
  }

  public JComponent createOpenDocumentUi() {
    return createComponent();
  }

  public JComponent createSaveDocumentUi() {
    return createComponent();
  }

  private JComponent createComponent() {
    OptionsPageBuilder builder = new OptionsPageBuilder();
    JPanel panel = new JPanel(new SpringLayout());
    panel.add(new JLabel(language.getText("fileFromServer")));
    panel.add(builder.createOptionComponent(null, myUrl));
    panel.add(new JLabel(language.getText("userName")));
    panel.add(builder.createOptionComponent(null, myUsername));
    panel.add(new JLabel(language.getText("password")));
    panel.add(builder.createOptionComponent(null, myPassword));
    addEmptyRow(panel);
    panel.add(new JLabel(language.getText("webdav.lockResource.label")));
    panel.add(builder.createOptionComponent(null, myLock));
    panel.add(new JLabel(language.getText("webdav.lockTimeout.label")));
    final Component timeoutComponent = builder.createOptionComponent(null, myTimeout);
    myLock.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        timeoutComponent.setEnabled(myLock.isChecked());
      }
    });
    panel.add(timeoutComponent);
    SpringUtilities.makeCompactGrid(panel, 6, 2, 0, 0, 10, 5);

    JPanel result = new JPanel(new BorderLayout());
    result.add(panel, BorderLayout.NORTH);
    result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    return result;
  }

  private static void addEmptyRow(JPanel form) {
    form.add(Box.createRigidArea(new Dimension(1, 10)));
    form.add(Box.createRigidArea(new Dimension(1, 10)));
  }
}
