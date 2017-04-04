package biz.ganttproject.impex.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author akurutin on 04.04.2017.
 */
public class XlsWriterImpl implements XlsWriter {
    private Workbook workbook;
    private Sheet sheet;
    private final OutputStream stream;


    private Row currentRow = null;
    private int nextRowInd = 0;
    private int nextCellInd = 0;


    public XlsWriterImpl(OutputStream stream, CSVFormat format) {
        this.stream = stream;
        workbook = new HSSFWorkbook();
        sheet = workbook.createSheet();
    }

    @Override
    public void print(Object value) throws IOException {
        if (currentRow == null) {
            createNewRow();
        }

        Cell cell = currentRow.createCell(nextCellInd++);
        if (value!=null) {
            cell.setCellValue(value.toString());
        }
    }

    @Override
    public void println() throws IOException {
        createNewRow();
        nextCellInd = 0;
    }

    @Override
    public void close() throws IOException {
        workbook.close();
        stream.close();
    }

    @Override
    public void flush() throws IOException {
        workbook.write(stream);
    }

    private void createNewRow() {
        currentRow = sheet.createRow(nextRowInd++);
    }
}
