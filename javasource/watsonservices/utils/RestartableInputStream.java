package watsonservices.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;

/**
 * {@link FileInputStream} wrapper which can reopen a stream after it was closed.
 * If {@link okhttp3.logging.HttpLoggingInterceptor} tries to log the request body,
 * {@link com.ibm.watson.developer_cloud.http.InputStreamRequestBody#writeTo(okio.BufferedSink)} closes the stream
 * before it's uploaded to Watson.
 */
public class RestartableInputStream extends FilterInputStream {

    private final File file;

    public RestartableInputStream(File file) throws FileNotFoundException {
        super(null);
        this.file = file;
        openStreamIfNeeded();
    }

    private void openStreamIfNeeded() throws FileNotFoundException {
        if (in == null) {
            in = new FileInputStream(file);
        }
    }

    @Override
    public int read() throws IOException {
        openStreamIfNeeded();
        return super.read();
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        openStreamIfNeeded();
        return super.read(bytes);
    }

    @Override
    public int read(byte[] bytes, int i, int i1) throws IOException {
        openStreamIfNeeded();
        return super.read(bytes, i, i1);
    }

    @Override
    public long skip(long l) throws IOException {
        openStreamIfNeeded();
        return super.skip(l);
    }

    @Override
    public int available() throws IOException {
        openStreamIfNeeded();
        return super.available();
    }

    @Override
    public synchronized void mark(int i) {
    }

    @Override
    public synchronized void reset() throws IOException {
        openStreamIfNeeded();
        super.reset();
    }

    @Override
    public boolean markSupported() {
        // Default for FileInputStream
        return false;
    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
        } finally {
            in = null;
        }
    }

}
