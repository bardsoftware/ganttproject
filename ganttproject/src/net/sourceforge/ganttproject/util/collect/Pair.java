package net.sourceforge.ganttproject.util.collect;

/**
 * @author Dmitry Barashev
 */
public class Pair<F,S> {
    private final F myFirst;
    private final S mySecond;

    private Pair(F first, S second) {
        myFirst = first;
        mySecond = second;
    }

    public F first() {
        return myFirst;
    }

    public S second() {
        return mySecond;
    }

    public static final <F,S> Pair<F,S> create(F first, S second) {
        return new Pair<F, S>(first, second);
    }
}