/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.DialogAligner;

public class GanttSplash extends JFrame {

    private JLabel mySplashComponent;

    public GanttSplash() {
        super("GanttProject Start");

        ImageIcon splashImage = new ImageIcon(getClass().getResource(
                "/icons/splash.png"));
        mySplashComponent = new JLabel(splashImage) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Font font = Fonts.SPLASH_FONT;
                g2.setFont(font);

                drawTextWithShadow(g2, GPVersion.PRAHA, 65, 80);
                //drawTextWithShadow(g2, "ganttproject.biz", 40, 287);
            }
        };
    }

    private void drawTextWithShadow(Graphics2D graphics, String text, int xpos, int ypos) {
        graphics.setColor(Color.GRAY);
        graphics.drawString(text, xpos , ypos);
    }
    @Override
    public void setVisible(boolean b) {
        if (b) {
            getContentPane().add(mySplashComponent, BorderLayout.CENTER);
            pack();
            DialogAligner.center(this);
        }
        super.setVisible(b);
    }

    @Override
    protected void frameInit() {
        super.frameInit();
        ImageIcon icon = new ImageIcon(getClass().getResource(
                "/icons/ganttproject.png"));
        setIconImage(icon.getImage()); // set the ganttproject icon
        setUndecorated(true);
    }

    public void close() {
        setVisible(false);
        dispose();
    }

    public JLabel getSplashComponent() {
        return mySplashComponent;
    }
}
