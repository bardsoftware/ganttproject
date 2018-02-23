package net.sourceforge.ganttproject.search;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Created by Doug Kelly on 2/22/2018.
 */

public class SearchResultCellRenderer implements ListCellRenderer {

    protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

    @Override
    public Component getListCellRendererComponent(JList jList, Object o, int index, boolean b, boolean b1) {
        StringBuilder theText = new StringBuilder();
        theText.append("<html>");

        JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(jList, o, index,
                b, b1);

        if (o instanceof SearchResult) {
            SearchResult searchResult = (SearchResult) o;
            theText.append("<b>" + searchResult.getTypeOfResult() + " #" + (index + 1) + "</b>: ");
            String label = searchResult.getLabel();
            String searchTerm = searchResult.getMyQueryMatch();
            theText.append(label.replaceAll("(?i)" + searchTerm + "", "<b><i>$0</i></b>"));
            if (!searchResult.getSecondaryLabel().isEmpty()) {
                theText.append("<br>");
                theText.append("&nbsp;<b>" + searchResult.getSecondaryLabel() + "</b>: ");
                String secondaryText = searchResult.getSecondaryText();
                theText.append(searchResult.getSecondaryText().replaceAll("(?i)" + searchTerm + "", "<b><i>$0</i></b>"));
            }
        } else {
            theText.append(o.toString());
        }
        theText.append("</html>");
        renderer.setText(theText.toString());
        renderer.setFont(new Font("Helvetica", Font.PLAIN, 12));
        return renderer;
    }
}
