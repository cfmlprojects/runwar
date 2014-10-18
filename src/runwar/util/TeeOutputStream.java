package runwar.util;
import java.io.OutputStream;
import java.io.IOException;

import com.sun.jmx.mbeanserver.Util;
 
/**
 * An OutputStream that sends bytes written to it to multiple output streams
 * in much the same way as the UNIX 'tee' command.
 *
 * @author Sabre150
 */
public class TeeOutputStream extends OutputStream
{
    /**
     * Constructs from a varags set of output streams
     *
     * @param ostream... the varags array of OutputStreams
     */
    public TeeOutputStream(OutputStream... ostream)
    {
        ostream_ = ostream;
    }
 
    /**
     * Writes a byte to both output streams
     *
     * @param b the byte to write
     * @throws IOException from any of the OutputStreams
     */
    @Override
    public void write(int b) throws IOException
    {
        for (OutputStream ostream : ostream_)
        {
            ostream.write(b);
        }
    }
 
    /**
     * Writes an array of bytes to all OutputStreams
     *
     * @param b the bytes to write
     * @param off the offset to start writing from
     * @param len the number of bytes to write
     * @throws IOException from any of the OutputStreams
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        for (OutputStream ostream : ostream_)
        {
            ostream.write(b, off, len);
        }
    }
 
    @Override
    public void flush() throws IOException
    {
        for (OutputStream ostream : ostream_)
        {
            ostream.flush();
        }
    }
 
    @Override
    public void close() throws IOException
    {
        for (OutputStream ostream : ostream_)
        {
            ostream.flush();
            ostream.close();
        }
    }
    // Obvious
    private final OutputStream[] ostream_;
}