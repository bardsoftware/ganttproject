package biz.ganttproject.impex.csv;

import com.google.common.base.Charsets;
import net.sourceforge.ganttproject.io.CSVOptions;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jdesktop.swingx.JXSearchField;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * @author akurutin on 04.04.2017.
 */
public class CsvWriterImpl implements CsvWriter {
    private final CSVPrinter csvPrinter;

    public CsvWriterImpl(OutputStream stream, CSVFormat format) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(stream, Charsets.UTF_8);
        csvPrinter = new CSVPrinter(writer,format);
    }

    @Override
    public void print(Object value) throws IOException {
        csvPrinter.print(value);
    }

    @Override
    public void println() throws IOException {
        csvPrinter.println();
    }

    @Override
    public void close() throws IOException {
        csvPrinter.close();
    }

    @Override
    public void flush() throws IOException {
        csvPrinter.flush();
    }
}
