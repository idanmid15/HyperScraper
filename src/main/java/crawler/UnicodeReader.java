package crawler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;

public class UnicodeReader
        extends Reader
{
    private static final int BOM_MAX_SIZE = 4;
    private InputStreamReader delegate;

    public UnicodeReader(InputStream in)
            throws IOException
    {
        init(in, null);
    }

    public UnicodeReader(InputStream in, String defaultEnc)
            throws IOException
    {
        init(in, defaultEnc);
    }

    public String getEncoding()
    {
        return this.delegate.getEncoding();
    }

    private void init(InputStream in, String defaultEnc)
            throws IOException
    {
        byte[] bom = new byte[4];

        PushbackInputStream internalIn = new PushbackInputStream(in, 4);
        int n = internalIn.read(bom, 0, bom.length);
        String encoding;
        int unread;
        if ((bom[0] == -17) && (bom[1] == -69) && (bom[2] == -65))
        {
            encoding = "UTF-8";
            unread = n - 3;
        }
        else
        {
            if ((bom[0] == -2) && (bom[1] == -1))
            {
                encoding = "UTF-16BE";
                unread = n - 2;
            }
            else
            {
                if ((bom[0] == -1) && (bom[1] == -2))
                {
                    encoding = "UTF-16LE";
                    unread = n - 2;
                }
                else
                {
                    if ((bom[0] == 0) && (bom[1] == 0) && (bom[2] == -2) && (bom[3] == -1))
                    {
                        encoding = "UTF-32BE";
                        unread = n - 4;
                    }
                    else
                    {
                        if ((bom[0] == -1) && (bom[1] == -2) && (bom[2] == 0) && (bom[3] == 0))
                        {
                            encoding = "UTF-32LE";
                            unread = n - 4;
                        }
                        else
                        {
                            encoding = defaultEnc;
                            unread = n;
                        }
                    }
                }
            }
        }
        if (unread > 0) {
            internalIn.unread(bom, n - unread, unread);
        } else if (unread < -1) {
            internalIn.unread(bom, 0, 0);
        }
        if (encoding == null) {
            this.delegate = new InputStreamReader(internalIn);
        } else {
            this.delegate = new InputStreamReader(internalIn, encoding);
        }
    }

    public void close()
            throws IOException
    {
        this.delegate.close();
    }

    public int read(char[] cbuf, int off, int len)
            throws IOException
    {
        return this.delegate.read(cbuf, off, len);
    }
}
