package biz.ganttproject.impex.csv;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * @author akurutin on 04.04.2017.
 */
public interface CommonWriter extends Flushable, Closeable {
    public void print(Object value) throws IOException;
    public void println() throws IOException;
    public void close() throws IOException;
    public void flush() throws IOException;
}
