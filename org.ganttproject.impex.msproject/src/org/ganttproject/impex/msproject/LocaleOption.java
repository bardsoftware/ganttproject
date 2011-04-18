/*
 * Created on 07.12.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ganttproject.impex.msproject;

import java.util.Locale;

import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.language.GanttLanguage;

class LocaleOption extends DefaultEnumerationOption<Locale> {
    private final Locale[] LOCALES = new Locale[] {Locale.FRANCE, Locale.US, new Locale("pt", "PT")};

    private Locale myLocale;

    LocaleOption() {
        super("impex.msproject.mpx.language", new String[] {
                Locale.FRANCE.getDisplayLanguage(GanttLanguage.getInstance().getLocale()), Locale.US.getDisplayLanguage(GanttLanguage.getInstance().getLocale()), new Locale("pt", "PT").getDisplayLanguage(GanttLanguage.getInstance().getLocale())
        });
    }

    public void commit() {
        super.commit();
        setSelectedLocale(getValue());
    }

    Locale getSelectedLocale() {
        return myLocale;
    }

    private void setSelectedLocale(String value) {
        for (int i=0; i<LOCALES.length; i++) {
            if (LOCALES[i].getDisplayLanguage(GanttLanguage.getInstance().getLocale()).equals(value)) {
                myLocale = LOCALES[i];
                break;
            }
        }
    }

    public void setSelectedLocale(Locale locale) {
        setValue(locale.getDisplayLanguage(GanttLanguage.getInstance().getLocale()));
    }
}
