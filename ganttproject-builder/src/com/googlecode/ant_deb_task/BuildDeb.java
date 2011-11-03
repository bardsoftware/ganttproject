package com.googlecode.ant_deb_task;

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

public class BuildDeb
{
    private static final String FILE_HEADER_FORMAT = "{0}{1}0     0     100644  {2}`\n";

    private static final String DEBIAN_BINARY_CONTENT = "2.0\n";
    private static final String DEBIAN_BINARY_NAME = "debian-binary";
    private static final String CONTROL_NAME = "control.tar.gz";
    private static final String DATA_NAME = "data.tar.gz";

    public static void buildDeb(File debFile, File controlFile, File dataFile) throws IOException
    {
        long now = new Date().getTime() / 1000;
        OutputStream deb = new FileOutputStream (debFile);

        deb.write("!<arch>\n".getBytes ());

        startFileEntry (deb, DEBIAN_BINARY_NAME, now, DEBIAN_BINARY_CONTENT.length());
        deb.write(DEBIAN_BINARY_CONTENT.getBytes ());
        endFileEntry (deb, DEBIAN_BINARY_CONTENT.length());

        startFileEntry (deb, CONTROL_NAME, now, controlFile.length());

        FileInputStream control = new FileInputStream(controlFile);
        byte[] buffer = new byte[1024];
        while(true)
        {
            int read = control.read(buffer);
            if (read == -1)
                break;
            deb.write(buffer, 0, read);
        }
        control.close();

        endFileEntry (deb, controlFile.length());

        startFileEntry (deb, DATA_NAME, now, dataFile.length());

        FileInputStream data = new FileInputStream(dataFile);
        while(true)
        {
            int read = data.read(buffer);
            if (read == -1)
                break;
            deb.write(buffer, 0, read);
        }
        data.close();

        endFileEntry (deb, dataFile.length());

        deb.close();
    }

    private static void startFileEntry(OutputStream deb, String name, long time, long length) throws IOException
    {
        String fileHeader = MessageFormat.format (
                FILE_HEADER_FORMAT,
                new Object[]{
                        padd (name, 16),
                        padd (time, 12),
                        padd (length, 10)
                }
        );

        deb.write(fileHeader.getBytes ());
    }

    private static void endFileEntry(OutputStream deb, long length) throws IOException
    {
        if (length % 2 == 1)
            deb.write("\n".getBytes ());
    }

    private static String padd(long number, int length)
    {
        return padd(Long.toString(number), length);
    }

    private static String padd(String text, int length)
    {
        StringBuffer buffer = new StringBuffer(text);

        for (int i = 0; i < length - text.length(); i++)
        {
            buffer.append(' ');
        }

        return buffer.toString();
    }

    public static void main(String[] args) throws IOException
    {
        buildDeb(new File(args[0]), new File(args[1]), new File(args[2]));
    }
}

