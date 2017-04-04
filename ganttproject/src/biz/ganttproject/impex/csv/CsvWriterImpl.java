package biz.ganttproject.impex.csv;

import com.google.common.base.Charsets;
import net.sourceforge.ganttproject.io.CSVOptions;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created with IntelliJ IDEA.
 * User: akurutin
 * Date: 03.04.17
 * Time: 19:48
 */
public class CsvWriterImpl implements CsvWriter {
  private final CSVPrinter csvPrinter;

  public CsvWriterImpl(OutputStream stream, CSVOptions csvOptions) throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(stream, Charsets.UTF_8);
    CSVFormat format = CSVFormat.DEFAULT.withEscape('\\');
    if (csvOptions.sSeparatedChar.length() == 1) {
      format = format.withDelimiter(csvOptions.sSeparatedChar.charAt(0));
    }
    if (csvOptions.sSeparatedTextChar.length() == 1) {
      format = format.withQuote(csvOptions.sSeparatedTextChar.charAt(0));
    }

    csvPrinter = new CSVPrinter(writer, format);
  }

  @Override
  public void print(String value) throws IOException {
    csvPrinter.print(value);
  }

  @Override
  public void println() throws IOException {
    csvPrinter.println();
  }

  @Override
  public void flush() throws IOException {
    csvPrinter.flush();
  }

  @Override
  public void close() throws IOException {
    csvPrinter.close();
  }
}
