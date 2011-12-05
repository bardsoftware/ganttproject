package com.googlecode.ant_deb_task;

import java.io.*;
import java.util.zip.*;

/**
 * An enhanced GZIP output stream that allows the compression level to be set.
 *
 * This class also allows the original filesystem to be specified. The original
 * java.util.zip.GZIPOutputStream defaults this to FAT, so we do the same here.
 *
 * @author Kevin McGuinness
 */
class GZipOutputStream extends DeflaterOutputStream
{
    // Much of this is just copied from the source from GZIPOutputStream
    private final static int GZIP_MAGIC = 0x8b1f;
    private final static int TRAILER_SIZE = 8;

    // These values are from RFC-1952
    public static final byte FS_FAT = 0;
    public static final byte FS_AMIGA = 1;
    public static final byte FS_VMS = 2;
    public static final byte FS_UNIX = 3;
    public static final byte FS_VM_CMS = 4;
    public static final byte FS_ATARI = 5;
    public static final byte FS_HPFS = 6;
    public static final byte FS_MAC = 7;
    public static final byte FS_Z_SYSTEM = 8;
    public static final byte FS_CPM = 9;
    public static final byte FS_TOPS_20 = 10;
    public static final byte FS_NTFS = 11;
    public static final byte FS_QDOS = 12;
    public static final byte FS_ACORN_RISC = 11;
    public static final byte FS_UNKNOWN = (byte) 0xff;

    protected CRC32 crc = new CRC32 ();
    private boolean closed = false;
    private int level;
    private byte fileSystem = FS_FAT;
    private boolean headerWritten = false;

    /**
     * Creates a new output stream with the specified buffer size.
     *
     * @param out
     *            the output stream
     * @param level
     *            the compression level (1..9)
     * @throws IOException
     *             If an I/O error has occurred.
     * @see java.util.zip.Deflater#setLevel(int)
     */
    public GZipOutputStream (OutputStream out, int level) throws IOException {
        super (out, new Deflater (level, true), 512);
        this.level = level;
        this.fileSystem = FS_FAT;
        crc.reset ();
    }

    /**
     * Set the file system that is used by the archive. This identifies the type
     * of file system on which compression took place. This may be useful in
     * determining end-of-line convention for text files.
     *
     * @param value
     *            One of the FS_* values (ex. FS_UNIX).
     */
    public void setFileSystem(byte value)
    {
        fileSystem = value;
    }

    public void close() throws IOException
    {
        if (!closed)
        {
            super.close();
            def.end ();
            closed = true;
        }
    }

    public synchronized void write (byte[] buf, int off, int len)
        throws IOException
    {
        if (!headerWritten)
        {
            writeHeader ();
        }

        super.write (buf, off, len);
        crc.update (buf, off, len);
    }

    public void finish () throws IOException
    {
        if (!headerWritten)
        {
            writeHeader ();
        }

        if (!def.finished ())
        {
            def.finish ();
            while (!def.finished ())
            {
                int len = def.deflate (buf, 0, buf.length);
                if (def.finished () && len <= buf.length - TRAILER_SIZE)
                {
                    // last deflater buffer. Fit trailer at the end
                    writeTrailer (buf, len);
                    len = len + TRAILER_SIZE;
                    out.write (buf, 0, len);
                    return;
                }
                if (len > 0)
                    out.write (buf, 0, len);
            }
            // if we can't fit the trailer at the end of the last
            // deflater buffer, we write it separately
            byte[] trailer = new byte[TRAILER_SIZE];
            writeTrailer (trailer, 0);
            out.write (trailer);
        }
    }

    private void writeHeader () throws IOException
    {
        byte[] header = new byte[10];

        // Magic number
        header[0] = (byte) GZIP_MAGIC;
        header[1] = (byte) (GZIP_MAGIC >> 8);

        // Compression method
        header[2] = Deflater.DEFLATED;

        // Flags
        header[3] = 0;

        // Modification time (unavailable)
        header[4] = 0;
        header[5] = 0;
        header[6] = 0;
        header[7] = 0;

        // Extra flags
        switch (level)
        {
        case Deflater.BEST_COMPRESSION:
            header[8] = 2;
            break;
        case Deflater.BEST_SPEED:
            header[8] = 4;
            break;
        default:
            header[8] = 0;
        }

        // Set OS
        header[9] = fileSystem;

        // Write
        out.write (header);
        headerWritten = true;
    }

    private void writeTrailer (byte[] buf, int offset) throws IOException
    {
        writeInt ((int) crc.getValue (), buf, offset);
        writeInt (def.getTotalIn (), buf, offset + 4);
    }

    private static void writeInt (int i, byte[] buf, int offset) throws IOException
    {
        writeShort (i & 0xffff, buf, offset);
        writeShort ((i >> 16) & 0xffff, buf, offset + 2);
    }

    private static void writeShort (int s, byte[] buf, int offset) throws IOException
    {
        buf[offset] = (byte) (s & 0xff);
        buf[offset + 1] = (byte) ((s >> 8) & 0xff);
    }
}