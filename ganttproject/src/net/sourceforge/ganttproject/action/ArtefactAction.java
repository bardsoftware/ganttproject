/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Abstract class which provides a base implementation for the artefact actions.
 * Depending on the visible chart, the name, description and action will change
 */
public abstract class ArtefactAction extends GPAction implements ActionStateChangedListener {
    private final ActiveActionProvider myProvider;

    public ArtefactAction(String name, ActiveActionProvider provider, ActionDelegate[] delegates) {
        super(name);
        myProvider = provider;
        for(ActionDelegate delegate : delegates) {
            delegate.addStateChangedListener(this);
        }

        // Make action state equal to active delegate action state
        actionStateChanged();
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

    public void actionStateChanged() {
        // State of a delegate action has been changed, so update out state as well
        GPAction activeAction = (GPAction) myProvider.getActiveAction();
        setEnabled(activeAction.isEnabled());
    }
}
