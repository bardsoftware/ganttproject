/*
 * Created on 26.12.2004
 */
package net.sourceforge.ganttproject.util;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * @author bard
 */
public class TextLengthCalculatorImpl implements TextLengthCalculator {
    private Graphics2D myGraphics;

    private State myState;

    public static int getTextLength(Graphics g, String text) {
        if(text.length() == 0)
            return 0;
        Graphics2D g2 = (Graphics2D) g;
        FontRenderContext frc = g2.getFontRenderContext();
        Font font = g.getFont();
        TextLayout layout = new TextLayout(text, font, frc);
        Rectangle2D bounds = layout.getBounds();
        return (int) bounds.getWidth() + 1;
    }

    public TextLengthCalculatorImpl(Graphics g) {
        if (g != null) {
            setGraphics(g);
        }
    }

    public void setGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        myGraphics = g2;
        myState = null;
    }

    public int getTextLength(String text) {
        return getTextLength(myGraphics, text);
    }

    public Object getState() {
        if (myState == null) {
            myState = new State(myGraphics.getFontRenderContext(), myGraphics
                    .getFont());
        }
        return myState;
    }

    static class State {
        Object context;

        Object font;

        State(Object context, Object font) {
            this.context = context;
            this.font = font;
            if (context == null) {
                throw new NullPointerException();
            }
            if (font == null) {
                throw new NullPointerException();
            }
        }

        public boolean equals(Object o) {
            State rvalue = (State) o;
            if (rvalue == null) {
                return false;
            }
            return rvalue.context.equals(this.context)
                    && rvalue.font.equals(this.font);
        }

        public int hashCode() {
            return font.hashCode();
        }
    }

}
