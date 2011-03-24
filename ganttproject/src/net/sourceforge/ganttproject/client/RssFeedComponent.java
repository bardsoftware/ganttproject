package net.sourceforge.ganttproject.client;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.util.BrowserControl;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

class RssFeedComponent {
    private final JPanel myComponent;
    private final Action[] myActions;
    int myPosition;
    private Action myBackwardAction;
    private Action myForwardAction;

    RssFeedComponent(RssFeed feed) {
        myComponent = new JPanel(new CardLayout());
        for (RssFeed.Item item : feed.getItems()) {
            myComponent.add(createItemComponent(item), String.valueOf(myComponent.getComponentCount()));
        }
        List<Action> actions = new ArrayList<Action>();
        if (feed.getItems().size() > 1) {
            myBackwardAction = createBackwardAction();
            myForwardAction = createForwardAction();
            actions.add(myBackwardAction);
            actions.add(myForwardAction);
            myActions = actions.toArray(new Action[0]);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateEnabled();
                }
            });
        } else {
            myActions = new Action[0];
        }

    }

    private Action createBackwardAction() {
        return new GPAction("updateRss.backwardItem") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (myPosition > 0) {
                    ((CardLayout)myComponent.getLayout()).show(myComponent, String.valueOf(--myPosition));
                    updateEnabled();
                }
            }
        };

    }

    private Action createForwardAction() {
        return new GPAction("updateRss.forwardItem") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (myPosition < myComponent.getComponentCount()-1) {
                    ((CardLayout)myComponent.getLayout()).show(myComponent, String.valueOf(++myPosition));
                    updateEnabled();
                }
            }
        };
    }

    private void updateEnabled() {
        assert myBackwardAction != null && myForwardAction != null;
        System.out.println("position=" + myPosition);
        myBackwardAction.setEnabled(myPosition > 0);
        myForwardAction.setEnabled(myPosition < myComponent.getComponentCount() - 1);
    }

    private Component createItemComponent(RssFeed.Item item) {
        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setEditorKit(new HTMLEditorKit());
        htmlPane.setEditable(false);
        htmlPane.setPreferredSize(new Dimension(300, 150));
        htmlPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    BrowserControl.displayURL(hyperlinkEvent.getURL().toString());
                }
            }
        });
        htmlPane.setBackground(Color.YELLOW);
        htmlPane.setText(MessageFormat.format("<html><body><b>{0}</b><br><p>{1}</p>", item.title, item.body));
        htmlPane.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane scrollPane = new JScrollPane(htmlPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    JComponent getComponent() {
        return myComponent;
    }

    Action[] getActions() {
        return myActions;
    }
}
