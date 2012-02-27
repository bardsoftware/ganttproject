/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * Abstract class which provides a base implementation for the artefact actions.
 * Depending on the visible chart, the name, description and action will change
 */
public class ArtefactAction extends GPAction implements ActionStateChangedListener {
    private final ActiveActionProvider myProvider;
    private final Action[] myDelegates;

    public ArtefactAction(String name, IconSize iconSize, ActiveActionProvider provider, Action[] delegates) {
        super(name, iconSize.asString());
        myProvider = provider;
        for (Action delegate : delegates) {
            delegate.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("enabled".equals(evt.getPropertyName())) {
                        actionStateChanged();
                    }
                }
            });
        }
        myDelegates = delegates;
        // Make action state equal to active delegate action state
        actionStateChanged();
    }

    @Override
    public GPAction withIcon(IconSize size) {
        return new ArtefactAction(getID(), size, myProvider, myDelegates);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AbstractAction activeAction = myProvider.getActiveAction();
        activeAction.actionPerformed(e);
    }

    @Override
    protected String getLocalizedName() {
        if(myProvider == null) {
            return super.getLocalizedName();
        }
        GPAction activeAction = (GPAction) myProvider.getActiveAction();
        return activeAction.getLocalizedName();
    };

    @Override
    protected String getLocalizedDescription() {
        if(myProvider == null) {
            return super.getLocalizedDescription();
        }
        GPAction activeAction = (GPAction) myProvider.getActiveAction();
        return activeAction.getLocalizedDescription();
    };

    @Override
    public void actionStateChanged() {
        // State of a delegate action has been changed, so update out state as well
        GPAction activeAction = (GPAction) myProvider.getActiveAction();
        setEnabled(activeAction.isEnabled());
        putValue(Action.SMALL_ICON, activeAction.getValue(Action.SMALL_ICON));
    }
}
