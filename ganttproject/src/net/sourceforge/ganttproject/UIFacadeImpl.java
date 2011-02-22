/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.gui.DialogAligner;
import net.sourceforge.ganttproject.gui.GanttDialogInfo;
import net.sourceforge.ganttproject.gui.GanttLookAndFeelInfo;
import net.sourceforge.ganttproject.gui.GanttLookAndFeels;
import net.sourceforge.ganttproject.gui.GanttStatusBar;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TaskSelectionContext;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder.I18N;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManagerImpl;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.ProgressProvider;

class UIFacadeImpl extends ProgressProvider implements UIFacade {
    private final JFrame myMainFrame;
    private final ScrollingManager myScrollingManager;
    private final ZoomManager myZoomManager;
    private final GanttStatusBar myStatusBar;
    private final UIFacade myFallbackDelegate;
    private final ErrorNotifier myErrorNotifier;
    private final TaskSelectionManager myTaskSelectionManager;
    private final GPOptionGroup myOptions;
    private final LafOption myLafOption;
    
    UIFacadeImpl(JFrame mainFrame, GanttStatusBar statusBar, IGanttProject project, UIFacade fallbackDelegate) {
        myMainFrame = mainFrame;
        myScrollingManager = new ScrollingManagerImpl();
        myZoomManager = new ZoomManager(project.getTimeUnitStack());
        myStatusBar = statusBar;
        myFallbackDelegate = fallbackDelegate;
        Job.getJobManager().setProgressProvider(this);
        myErrorNotifier = new ErrorNotifier(this);
        myTaskSelectionManager = new TaskSelectionManager();
        
        myLafOption = new LafOption(this);
        LanguageOption languageOption = new LanguageOption();
        GPOption[] options = new GPOption[] {myLafOption, languageOption};
        myOptions = new GPOptionGroup("ui", options);
        I18N i18n = new OptionsPageBuilder.I18N();
        myOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myLafOption), "looknfeel");
        myOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(languageOption), "language");
        myOptions.setTitled(false);
    }

    public ScrollingManager getScrollingManager() {
        return myScrollingManager;
    }

    public ZoomManager getZoomManager() {
        return myZoomManager;
    }
    
    public GPUndoManager getUndoManager() {
        return myFallbackDelegate.getUndoManager();
    }
    
    class MyDialog {
        boolean result;
        final Component contentComponent;
        final OkAction okAction = new OkAction() {
            public void actionPerformed(ActionEvent e) {
                result = true;
            }
        };

        final CancelAction cancelAction = new CancelAction() {
            public void actionPerformed(ActionEvent e) {
            }
        };

        MyDialog(int dialogType, String message) {
            this.contentComponent = createDialogContentComponent(dialogType, message);
        }
        void show() {
            showDialog(contentComponent, new Action[] {okAction, cancelAction});
        }
    }

    public Choice showConfirmationDialog(String message, String title) {
        String yes = GanttLanguage.getInstance().getText("yes");
        String no = GanttLanguage.getInstance().getText("no");
        String cancel = GanttLanguage.getInstance().getText("cancel");
        int result = JOptionPane.showOptionDialog(myMainFrame, message, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {yes, no, cancel}, yes);
        switch (result) {
        case JOptionPane.YES_OPTION: return Choice.YES;
        case JOptionPane.NO_OPTION: return Choice.NO;
        case JOptionPane.CANCEL_OPTION: return Choice.CANCEL;
        case JOptionPane.CLOSED_OPTION: return Choice.CANCEL;
        default: return Choice.CANCEL;
        }
    }

    public void showPopupMenu(Component invoker, Action[] actions, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        for (int i = 0; i < actions.length; i++) {
            Action next = actions[i];
            if (next == null) {
                menu.addSeparator();
            } else {
                menu.add(next);
            }
        }
        menu.applyComponentOrientation(getLanguage().getComponentOrientation());
        menu.show(invoker, x, y);
    }

    public void showDialog(Component content, Action[] buttonActions) {
    	showDialog(content, buttonActions, "");
    }

    public void showDialog(Component content, Action[] buttonActions, String title) {
        JDialog result = new JDialog(myMainFrame, true);
        result.setTitle(title);
        final Commiter commiter = new Commiter();
        Action cancelAction = null;
        Box buttonBox = Box.createHorizontalBox();
        for (int i = 0; i < buttonActions.length; i++) {
            Action nextAction = buttonActions[i];
            JButton nextButton = null;
            if (nextAction instanceof OkAction) {
                nextAction = createOkAction(nextAction, result, commiter);
                nextButton = new JButton(nextAction);
                result.getRootPane().setDefaultButton(nextButton);
            }
            if (nextAction instanceof CancelAction) {
                nextAction = createCancelAction(nextAction, result, commiter);
                cancelAction = nextAction;
                result.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                        nextAction.getValue(Action.NAME));
                result.getRootPane().getActionMap().put(
                        nextAction.getValue(Action.NAME), nextAction);
            }
            if (nextButton == null) {
                nextButton = new JButton(nextAction);
            }
            buttonBox.add(nextButton);
            if (i < buttonActions.length - 1) {
                buttonBox.add(Box.createHorizontalStrut(5));
            }
        }
        result.getContentPane().setLayout(new BorderLayout());
        result.getContentPane().add(content, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
        buttonPanel.add(buttonBox, BorderLayout.EAST);
        result.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        result.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final Action localCancelAction = cancelAction;
        result.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                if (localCancelAction != null && !commiter.isCommited()) {
                    localCancelAction.actionPerformed(null);
                }
            }
        });
        result.pack();

        DialogAligner.center(result, myMainFrame);
        result.setVisible(true);
    }

    public void setStatusText(String text) {
        myStatusBar.setFirstText(text, 2000);
    }

    public void showErrorDialog(String errorMessage) {
        if (myMainFrame.isVisible()) {
            GanttDialogInfo gdi = new GanttDialogInfo(myMainFrame,
                    GanttDialogInfo.ERROR, GanttDialogInfo.YES_OPTION, errorMessage,
                    getLanguage().getText("error"));
            gdi.setVisible(true);
        } else {
            System.err.println("[GanttProjectBase] showErrorDialog:\n "+errorMessage);
        }
    }

    public void showErrorDialog(Throwable e) {
        showErrorDialog(getExceptionReport(e));
        GPLogger.log(e);
    }

	public void logErrorMessage(Throwable e) {
		myErrorNotifier.add(e);
		myStatusBar.setErrorNotifier(myErrorNotifier);
	}
	
	void resetErrorLog() {
		myStatusBar.setErrorNotifier(null);
	}
	
	private Component createDialogContentComponent(int dialogType, String message) {
        JLabel label;
        switch (dialogType) {
        case GanttDialogInfo.ERROR:
            label = new JLabel(new ImageIcon(getClass().getResource(
            "/icons/error.png")));
            break;
        case GanttDialogInfo.WARNING:
            label = new JLabel(new ImageIcon(getClass().getResource(
                    "/icons/warning.png")));
            break;
        case GanttDialogInfo.INFO:
            label = new JLabel(new ImageIcon(getClass().getResource(
                    "/icons/info.png")));
            break;
        case GanttDialogInfo.QUESTION:
            label = new JLabel(new ImageIcon(getClass().getResource(
                    "/icons/question.png")));
            break;
            default: throw new IllegalStateException("We should not be here");
        }
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.add(label, BorderLayout.NORTH);

        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setBackground(new JLabel().getBackground());
        
        JPanel result = new JPanel(new BorderLayout());
        result.add(imagePanel, BorderLayout.WEST);
        result.add(textArea, BorderLayout.CENTER);
        return result;
    }
    
    public GanttChart getGanttChart() {
        return myFallbackDelegate.getGanttChart();
    }

    public Chart getResourceChart() {
        return myFallbackDelegate.getResourceChart();
    }

    public Chart getActiveChart() {
        return myFallbackDelegate.getActiveChart();
    }

    public int getViewIndex() {
        return myFallbackDelegate.getViewIndex();
    }

    public void setViewIndex(int viewIndex) {
        myFallbackDelegate.setViewIndex(viewIndex);
    }

    public int getGanttDividerLocation() {
        return myFallbackDelegate.getGanttDividerLocation();
    }

    public void setGanttDividerLocation(int location) {
        myFallbackDelegate.setGanttDividerLocation(location);
    }

    public int getResourceDividerLocation() {
        return myFallbackDelegate.getResourceDividerLocation();
    }

    public void setResourceDividerLocation(int location) {
        myFallbackDelegate.setResourceDividerLocation(location);
    }

    public void refresh() {
        myFallbackDelegate.refresh();
    }

    public Frame getMainFrame() {
        return myMainFrame;
    }


    private static class Commiter {
        void commit() {
            isCommited = true;
        }

        boolean isCommited() {
            return isCommited;
        }

        private boolean isCommited;
    }

    private class ProxyOkAction extends OkAction implements
            PropertyChangeListener {
        private Action myRealAction;

        private JDialog myDialog;

        private Commiter myCommiter;

        private ProxyOkAction(final Action realAction, final JDialog dialog,
                final Commiter commiter) {
            realAction.addPropertyChangeListener(this);
            myRealAction = realAction;
            myDialog = dialog;
            myCommiter = commiter;
            setEnabled(realAction.isEnabled());
        }

        public void actionPerformed(ActionEvent e) {
            myRealAction.removePropertyChangeListener(this);
            myRealAction.actionPerformed(e);
            myCommiter.commit();
            if (myDialog.isVisible()) {
                myDialog.setVisible(false);
                myDialog.dispose();
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            setEnabled(myRealAction.isEnabled());
        }

    }

    private Action createOkAction(final Action realAction,
            final JDialog dialog, final Commiter commiter) {
        return new ProxyOkAction(realAction, dialog, commiter);
    }

    private Action createCancelAction(final Action realAction,
            final JDialog result, final Commiter commiter) {
        return new CancelAction() {
            public void actionPerformed(ActionEvent e) {
                realAction.actionPerformed(e);
                commiter.commit();
                result.setVisible(false);
                result.dispose();
            }
        };
    }

    private static GanttLanguage getLanguage() {
        return GanttLanguage.getInstance();
    }
    
    static String getExceptionReport(Throwable e) {
        StringBuffer result = new StringBuffer();
        result.append(e.getMessage() + "\n\n");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        e.printStackTrace(writer);
        writer.close();
        result.append(stringWriter.getBuffer().toString());
        return result.toString();
    }

    public void setWorkbenchTitle(String title) {
        myMainFrame.setTitle(title);        
    }
        
    public IProgressMonitor createMonitor(Job job) {
        return myStatusBar.createProgressMonitor();
    }

    public IProgressMonitor createProgressGroup() {
        return myStatusBar.createProgressMonitor();
    }

    public IProgressMonitor createMonitor(Job job, IProgressMonitor group, int ticks) {
        return group;
    }

    public IProgressMonitor getDefaultMonitor() {
        return null;
    }

	public TaskTreeUIFacade getTaskTree() {
		return myFallbackDelegate.getTaskTree();
	}

	public ResourceTreeUIFacade getResourceTree() {
		return myFallbackDelegate.getResourceTree();
	}

	public TaskSelectionContext getTaskSelectionContext() {
		return myTaskSelectionManager;
	}

    public TaskSelectionManager getTaskSelectionManager() {
        return myTaskSelectionManager;
    }

    public GanttLookAndFeelInfo getLookAndFeel() {
        return myLafOption.getLookAndFeel();
    }

    @Override
    public void setLookAndFeel(final GanttLookAndFeelInfo laf) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!doSetLookAndFeel(laf)) {
                    doSetLookAndFeel(GanttLookAndFeels.getGanttLookAndFeels().getDefaultInfo());
                }
            }
        });
    }
    private boolean doSetLookAndFeel(GanttLookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
            SwingUtilities.updateComponentTreeUI(myMainFrame);
            return true;
        } catch (Exception e) {
            GPLogger.getLogger(UIFacade.class).log(
                Level.SEVERE, "Can't find the LookAndFeel\n" + laf.getClassName() + "\n" + laf.getName());
            return false;
        }
    }

    static class LafOption extends DefaultEnumerationOption<GanttLookAndFeelInfo> implements GP1XOptionConverter {
        private final UIFacade myUiFacade;
        LafOption(UIFacade uiFacade) {
            super("laf", GanttLookAndFeels.getGanttLookAndFeels().getInstalledLookAndFeels());
            myUiFacade = uiFacade;
        }
        public GanttLookAndFeelInfo getLookAndFeel() {
            return GanttLookAndFeels.getGanttLookAndFeels().getInfoByName(getValue());            
        }
        @Override
        protected String objectToString(GanttLookAndFeelInfo laf) {
            return laf.getName();
        }
        @Override
        public void commit() {
            super.commit();
            myUiFacade.setLookAndFeel(GanttLookAndFeels.getGanttLookAndFeels().getInfoByName(getValue()));
        }
        @Override
        public String getTagName() {
            return "looknfeel";
        }
        @Override
        public String getAttributeName() {
            return "name";
        }
        @Override
        public void loadValue(String legacyValue) {
            setValue(legacyValue, true);
            myUiFacade.setLookAndFeel(GanttLookAndFeels.getGanttLookAndFeels().getInfoByName(legacyValue));
        }
    }
    
    static class LanguageOption extends DefaultEnumerationOption<Locale> implements GP1XOptionConverter {
        public LanguageOption() {
            super("language", GanttLanguage.getInstance().getAvailableLocales().toArray(new Locale[0]));             
        }
        @Override
        protected String objectToString(Locale locale) {
            String englishName = locale.getDisplayLanguage(Locale.US);
            String localName = locale.getDisplayLanguage(locale);
            if ("en".equals(locale.getLanguage()) || "zh".equals(locale.getLanguage())) {
                if (!locale.getCountry().isEmpty()) {
                    englishName += " - " + locale.getDisplayCountry(Locale.US);
                    localName += " - " + locale.getDisplayCountry(locale);
                }                
            }
            if (localName.equals(englishName)) {
                return englishName;
            }
            return englishName + " (" + localName + ")";
        }
        @Override
        public void commit() {
            Locale l = (Locale) stringToObject(getValue());
            GanttLanguage.getInstance().setLocale(l);
        }
        @Override
        public String getTagName() {
            return "language";
        }
        @Override
        public String getAttributeName() {
            return "selection";
        }
        @Override
        public void loadValue(String legacyValue) {
            loadPersistentValue(legacyValue);
        }
        @Override
        public String getPersistentValue() {
            Locale l = (Locale) stringToObject(getValue());
            assert l != null;
            String result = l.getLanguage();
            if (!l.getCountry().isEmpty()) {
                result += "_" + l.getCountry();
            }
            return result;
        }
        @Override
        public void loadPersistentValue(String value) {
            String[] lang_country = value.split("_");
            Locale l;
            if (lang_country.length == 2) {
                l = new Locale(lang_country[0], lang_country[1]);
            } else {
                l = new Locale(lang_country[0]);
            }
            value = objectToString(l);
            if (value != null) {
                setValue(value, true);
            }
        }
        
    }

    @Override
    public GPOptionGroup getOptions() {
        return myOptions;
    }
}

