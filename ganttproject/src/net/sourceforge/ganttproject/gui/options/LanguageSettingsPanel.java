/***************************************************************************
 LanguageSettingsPanel
 ------------------------------------------
 begin                : 27 juin 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas Panel to choose the language for GanttProject
 */
public class LanguageSettingsPanel extends GeneralOptionPanel implements
        ItemListener {

    // combo box to store all languages data
    private final JComboBox cbLanguage;

    private final GanttProject appli;

    /** Constructor. */
    public LanguageSettingsPanel(GanttProject parent) {
        super(GanttLanguage.getInstance().getText("languages"), GanttLanguage
                .getInstance().getText("settingsLanguages"));

        appli = parent;

        // create the combo box with all languages
        cbLanguage = new JComboBox();
        cbLanguage.addItem("Traditional Chinese");
        cbLanguage.addItem("Simplified Chinese");
        cbLanguage
                .addItem("\u0411\u044a\u043b\u0433\u0430\u0440\u0441\u043a\u0438");
        cbLanguage.addItem("\u010cesky");
        cbLanguage.addItem("Dansk");
        cbLanguage.addItem("Deutsch");
        cbLanguage.addItem("English");
        cbLanguage.addItem("English (Australia)");
        cbLanguage.addItem("English (United Kingdom)");
        cbLanguage.addItem("Espa\u00f1ol");
        cbLanguage.addItem("Estonian");
        cbLanguage.addItem("Finnish");
        cbLanguage.addItem("Fran\u00e7ais");
        cbLanguage.addItem("Greek");
        cbLanguage.addItem("Hrvatski");
        cbLanguage.addItem("Hungarian");
        cbLanguage.addItem("Korean");
        cbLanguage.addItem("\u05e2\u05d1\u05e8\u05d9\u05ea");
        cbLanguage.addItem("Italiano");
        cbLanguage.addItem("Japanese");
        cbLanguage.addItem("Nederlands");
        cbLanguage.addItem("Norsk");
        cbLanguage.addItem("Polski");
        cbLanguage.addItem("Portugu\u00EAs");
        cbLanguage.addItem("Portugu\u00eas do Brasil");
        cbLanguage.addItem("\u0420\u0443\u0441\u0441\u043a\u0438\u0439");
        cbLanguage.addItem("Romanian");
        cbLanguage.addItem("Sloven\u0161\u010dina");
        cbLanguage.addItem("Slovensky");
        cbLanguage.addItem("Svenska");
        cbLanguage.addItem("T\u00FCrk\u00E7e");
        cbLanguage.addItem("Ti\u1ebfng anh");
        cbLanguage.addItemListener(this);

        JPanel languagePanel = new JPanel(new BorderLayout());
        languagePanel.add(cbLanguage, BorderLayout.NORTH);
        vb.add(languagePanel);

        applyComponentOrientation(language.getComponentOrientation());
    }

    /** This method check if the value has changed, and assk for commit changes. */
    public boolean applyChanges(boolean askForApply) {
        // if there is changes
        if (bHasChange) {
            if (!askForApply || (askForApply && askForApplyChanges()))
                changeLanguage();
        }
        return bHasChange;
    }

    /** Initialize the component. */
    public void initialize() {
        cbLanguage.setSelectedItem(language.getText("longLanguage").trim());
        bHasChange = false;
    }

    /** The language has changed. */
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED)
            this.bHasChange = true;
    }

    /** Apply the new language */
    public void changeLanguage() {
        String lang = (String) cbLanguage.getSelectedItem();

        if (lang.equals("Traditional Chinese")) {
            language.setLocale(Locale.TAIWAN);
        } else if (lang.equals("Simplified Chinese")) {
            language.setLocale(Locale.CHINA);
        } else if (lang
                .equals("\u0411\u044a\u043b\u0433\u0430\u0440\u0441\u043a\u0438")) {
            language.setLocale(new Locale("bg", "BG"));
        } else if (lang.equals("\u010cesky")) {
            language.setLocale(new Locale("cs", "CZ"));
        } else if (lang.equals("Dansk")) {
            language.setLocale(new Locale("da", "DK"));
        } else if (lang.equals("Deutsch")) {
            language.setLocale(Locale.GERMANY);
        } else if (lang.equals("English")) {
            language.setLocale(Locale.US);
        } else if (lang.equals("English (United Kingdom)")) {
            language.setLocale(Locale.UK);
        } else if (lang.equals("English (Australia)")) {
            language.setLocale(new Locale("en", "AU"));
        } else if (lang.equals("Espa\u00f1ol")) {
            language.setLocale(new Locale("es", "ES"));
        } else if (lang.equals("Estonian")) {
            language.setLocale(new Locale("et", "ET"));
        } else if (lang.equals("Greek")) {
            language.setLocale(new Locale("el", "GR"));
        } else if (lang.equals("Finnish")) {
            language.setLocale(new Locale("fi", "FI"));
        } else if (lang.equals("Fran\u00e7ais")) {
            language.setLocale(Locale.FRANCE);
        } else if (lang.equals("Hrvatski")) {
        	language.setLocale(new Locale("hr", "HR"));
        } else if (lang.equals("Hungarian")) {
            language.setLocale(new Locale("hu", "HU"));
        } else if (lang.equals("Korean")) {
            language.setLocale(new Locale("ko", "KR"));
        } else if (lang.equals("\u05e2\u05d1\u05e8\u05d9\u05ea")) {
            language.setLocale(new Locale("iw", "iW"));
        } else if (lang.equals("Italiano")) {
            language.setLocale(Locale.ITALY);
        } else if (lang.equals("Japanese")) {
            language.setLocale(new Locale("ja", "JP"));
        } else if (lang.equals("Nederlands")) {
            language.setLocale(new Locale("nl", "NL"));
        } else if (lang.equals("Norsk")) {
            language.setLocale(new Locale("no", "NO"));
        } else if (lang.equals("Polski")) {
            language.setLocale(new Locale("pl", "PL"));
        } else if (lang.equals("Portugu\u00EAs")) {
            language.setLocale(new Locale("pt", "PT"));
        } else if (lang.equals("Portugu\u00eas do Brasil")) {
            language.setLocale(new Locale("pt", "BR"));
        } else if (lang.equals("Romanian")) {
            language.setLocale(new Locale("ro", "RO"));
        } else if (lang.equals("Sloven\u0161\u010dina")) {
            language.setLocale(new Locale("sl", "SL"));
        } else if (lang.equals("Slovensky")) {
            language.setLocale(new Locale("sk", "SK"));
        } else if (lang.equals("Svenska")) {
            language.setLocale(new Locale("sv", "SV"));
        } else if (lang.equals("T\u00FCrk\u00E7e")) {
            language.setLocale(new Locale("tr", "TR"));
        } else if (lang.equals("\u0420\u0443\u0441\u0441\u043a\u0438\u0439")) {
            language.setLocale(new Locale("ru", "RU"));
        } else if (lang.equals("Ti\u1ebfng anh")) {
            language.setLocale(new Locale("vi", "VN"));
        }
        appli.changeLanguage();
    }
}
