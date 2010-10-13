package net.sourceforge.ganttproject.shape;

/*
 *@author Etienne L'kenfack (etienne.lkenfack@itcogita.com)
 */

public class ShapeConstants {
    public static final ShapePaint TRANSPARENT = new ShapePaint(4, 4,
            new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });

    public static final ShapePaint DEFAULT = new ShapePaint(4, 4, new int[] {
            1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1 });

    public static final ShapePaint CROSS = new ShapePaint(4, 4, new int[] { 0,
            1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 0 });

    public static final ShapePaint VERT = new ShapePaint(4, 4, new int[] { 1,
            0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0 });

    public static final ShapePaint HORZ = new ShapePaint(4, 4, new int[] { 0,
            0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1 });

    public static final ShapePaint GRID = new ShapePaint(4, 4, new int[] { 1,
            0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1 });

    public static final ShapePaint ROUND = new ShapePaint(4, 4, new int[] { 0,
            1, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0 });

    public static final ShapePaint NW_TRIANGLE = new ShapePaint(4, 4,
            new int[] { 1, 1, 1, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 });

    public static final ShapePaint NE_TRIANGLE = new ShapePaint(4, 4,
            new int[] { 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0 });

    public static final ShapePaint SW_TRIANGLE = new ShapePaint(4, 4,
            new int[] { 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0 });

    public static final ShapePaint SE_TRIANGLE = new ShapePaint(4, 4,
            new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1 });

    public static final ShapePaint DIAMOND = new ShapePaint(4, 4, new int[] {
            0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0 });

    public static final ShapePaint DOTS = new ShapePaint(4, 4, new int[] { 1,
            0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0 });

    public static final ShapePaint DOT = new ShapePaint(4, 4, new int[] { 1, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0 });

    public static final ShapePaint SLASH = new ShapePaint(4, 4, new int[] { 1,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 });

    public static final ShapePaint BACKSLASH = new ShapePaint(4, 4, new int[] {
            0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0 });

    public static final ShapePaint THICK_VERT = new ShapePaint(4, 4, new int[] {
            0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0 });

    public static final ShapePaint THICK_HORZ = new ShapePaint(4, 4, new int[] {
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0 });

    public static final ShapePaint THICK_GRID = new ShapePaint(4, 4, new int[] {
            0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0 });

    public static final ShapePaint THICK_SLASH = new ShapePaint(4, 4,
            new int[] { 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1 });

    public static final ShapePaint THICK_BACKSLASH = new ShapePaint(4, 4,
            new int[] { 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 0, 1 });

    public static ShapePaint[] PATTERN_LIST = { TRANSPARENT, DEFAULT, CROSS,
            VERT, HORZ, GRID, ROUND, NW_TRIANGLE, NE_TRIANGLE, SW_TRIANGLE,
            SE_TRIANGLE, DIAMOND, DOTS, DOT, SLASH, BACKSLASH, THICK_VERT,
            THICK_HORZ, THICK_GRID, THICK_SLASH, THICK_BACKSLASH };
}
