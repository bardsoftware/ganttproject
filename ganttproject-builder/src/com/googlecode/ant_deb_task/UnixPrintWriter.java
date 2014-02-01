package com.googlecode.ant_deb_task;

import java.io.*;

public class UnixPrintWriter extends PrintWriter
{
    public UnixPrintWriter (File file) throws FileNotFoundException
    {
        super (new FileOutputStream (file));
    }

    public UnixPrintWriter (Writer writer)
    {
        super (writer);
    }

    public UnixPrintWriter (Writer writer, boolean b)
    {
        super (writer, b);
    }

    public UnixPrintWriter (OutputStream outputStream)
    {
        super (outputStream);
    }

    public UnixPrintWriter (OutputStream outputStream, boolean b)
    {
        super (outputStream, b);
    }

    public void println ()
    {
        print ('\n');
    }
}
