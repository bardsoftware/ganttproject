package net.sourceforge.ganttproject.io;

import biz.ganttproject.impex.csv.CsvWriter;

/**
 * @author akurutin on 04.04.2017.
 */
public class CSVOptionsHandler {
    private CSVOptions csvOptions;
    private CSVOptions xlsOptions;

    public CSVOptionsHandler(CSVOptions csvOptions, CSVOptions xlsOptions) {
        this.csvOptions = csvOptions;
        this.xlsOptions = xlsOptions;
    }

    public CSVOptions getCsvOptions() {
        return csvOptions;
    }

    public CSVOptions getXlsOptions() {
        return xlsOptions;
    }
}
