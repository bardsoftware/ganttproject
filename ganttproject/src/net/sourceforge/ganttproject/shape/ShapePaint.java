package net.sourceforge.ganttproject.shape;

/*
 *@author Etienne L'kenfack (etienne.lkenfack@itcogita.com)
 */

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

public class ShapePaint extends TexturePaint {
    protected int width, height;

    protected int[] array;

    protected Color foreground, background;

    /** Constructor */
    public ShapePaint(ShapePaint pattern) {
        this(pattern.width, pattern.height, pattern.array, pattern.foreground,
                pattern.background);
    }

    /** Constructor */
    public ShapePaint(ShapePaint pattern, Color foreground, Color background) {
        this(pattern.width, pattern.height, pattern.array, foreground,
                background);
    }

    /** Constructor */
    public ShapePaint(int width, int height, int[] array) {
        this(width, height, array, Color.black, Color.white);
    }

    /** Constructor */
    public ShapePaint(int width, int height, int[] array, Color foreground,
            Color background) {
        super(createTexture(width, height, array, foreground, background),
                new Rectangle(0, 0, width, height));
        this.width = width;
        this.height = height;
        this.array = array;
        this.foreground = foreground;
        this.background = background;
    }

    private static BufferedImage createTexture(int width, int height,
            int[] array, Color foreground, Color background) {
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, array[x + y * width] > 0 ? foreground
                        .getRGB() : background.getRGB());
            }
        }
        return image;
    }

    /** Return true if the two shape are the same */
    public boolean equals(Object obj) {
        if (obj instanceof ShapePaint) {
            ShapePaint paint = (ShapePaint) obj;

            if (array.length != paint.array.length)
                return false;

            for (int i = 0; i < array.length; i++)
                if (array[i] != paint.array[i])
                    return false;

            return // paint.array.equals(array) &&
            paint.width == width && paint.height == height;
        }
        return false;
    }

    /** Just for debug */
    public String toString2() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[" + width);
        buffer.append("," + height);
        buffer.append("] {");
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                buffer.append(",");
            buffer.append("" + array[i]);
        }
        buffer.append("}");
        return buffer.toString();
    }

    /** Return a string for the shape */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("PatternPaint[");
        buffer.append("[width=" + width);
        buffer.append(",height=" + height);
        buffer.append(",array={");
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                buffer.append(",");
            buffer.append("" + array[i]);
        }
        buffer.append("},foreground=" + foreground);
        buffer.append(",background=" + background + "]");
        return buffer.toString();
    }

    /** Return the array of the shape on a string */
    public String getArray() {
        String result = "";
        if (array != null)
            for (int i = 0; i < array.length; i++) {
                result += array[i] + ",";
            }

        return (result.length() != 0) ? result.trim().substring(0,
                result.trim().length() - 1) : "";
    }

    /** Return the array of the shape */
    public int[] getarray() {
        return array;
    }

}
