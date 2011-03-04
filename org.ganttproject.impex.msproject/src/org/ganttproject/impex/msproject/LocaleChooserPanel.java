package org.ganttproject.impex.msproject;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.gui.options.GeneralOptionPanel;
import net.sourceforge.ganttproject.gui.options.TopPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

class LocaleChooserPanel extends JPanel {

    private LocalePanel localePanel = null;

    private static GanttLanguage lang = GanttLanguage.getInstance();

    public LocaleChooserPanel() {
        super();

        JPanel result = new JPanel(new BorderLayout());
        result.setBorder(new EmptyBorder(0, 5, 0, 5));
        JComponent topPanel = TopPanel.create(lang.getText("mpxLanguageSettings"),
                lang.getText("mpxLanguageSettingsComment"));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        result.add(topPanel, BorderLayout.NORTH);
        localePanel = new LocalePanel();
        result.add(localePanel, BorderLayout.CENTER);

        add(result);
    }

    public Locale getSelectedLocale() {
        return localePanel.getSelectedLocale();
    }

    static class LocalePanel extends GeneralOptionPanel {

        private static final String LOCALE_FR = "Fran�ais";

        private static final String LOCALE_EN = "English";

        private static Map<String, Locale> mapLocales = null;

        static {
            mapLocales = new HashMap<String, Locale>();
            mapLocales.put(LOCALE_EN, Locale.US);
            mapLocales.put(LOCALE_FR, Locale.FRANCE);
        }

        private JComboBox combo = null;

        public LocalePanel() {
            super("", "");
            combo = new JComboBox(new Vector<String>(mapLocales.keySet()));
            vb.add(combo);
            Locale currentLocale = GanttLanguage.getInstance().getLocale();
            try {
                combo.setSelectedItem(getString(currentLocale));
            } catch (Exception e) {
                if (!GPLogger.log(e)) {
                    e.printStackTrace(System.err);
                }
            }
        }

        public boolean applyChanges(boolean askForApply) {
            return false;
        }

        public void initialize() {
            // nothing
        }

        Locale getSelectedLocale() {
            return (Locale) mapLocales.get(combo.getSelectedItem());
        }

        private static String getString(Locale locale) {
            String res = null;
            Iterator<String> it = mapLocales.keySet().iterator();
            while (it.hasNext()) {
                res = it.next();
                if (mapLocales.get(res).equals(locale)) {
                    break;
                }
            }
            return res;
        }
    }
}
