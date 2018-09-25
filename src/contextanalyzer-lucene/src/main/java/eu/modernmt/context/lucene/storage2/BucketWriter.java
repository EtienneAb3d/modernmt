package eu.modernmt.context.lucene.storage2;

import eu.modernmt.io.FileSystemUtils;
import eu.modernmt.io.UTF8Charset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPOutputStream;

class BucketWriter implements Closeable {

    private static final long COMPRESS_THRESHOLD = 50L * 1024L; // 50 Kb

    private static FileOutputStream openStream(File path, long size) throws IOException {
        FileOutputStream stream = new FileOutputStream(path, true);

        FileChannel channel = stream.getChannel();
        channel.truncate(size);

        return stream;
    }

    private final Bucket bucket;
    private FileOutputStream stream = null;
    private boolean deleted = false;

    public BucketWriter(Bucket bucket) {
        this.bucket = bucket;
    }

    public void append(String line) throws IOException {
        if (deleted)
            throw new FileNotFoundException("Bucket is deleted");

        if (stream == null)
            stream = openStream(bucket.path, bucket.plainTextFileSize);

        stream.write(line.getBytes(UTF8Charset.get()));
        stream.write('\n');
    }

    public void flush() throws IOException {
        synchronized (bucket) {
            if (deleted) {
                this.bucket.plainTextFileSize = 0;
                this.bucket.compressedFileSize = 0;
                this.bucket.virtualSize = 0;

                IOUtils.closeQuietly(this.stream);
                this.stream = null;

                FileUtils.forceDelete(this.bucket.gzPath);
                FileUtils.forceDelete(this.bucket.path);
            } else if (stream != null) {
                FileSystemUtils.fsync(stream);

                long size = stream.getChannel().position();
                bucket.virtualSize += size - bucket.plainTextFileSize;

                // Compress if needed
                if (size >= COMPRESS_THRESHOLD) {
                    IOUtils.closeQuietly(this.stream);
                    this.stream = null;

                    bucket.compressedFileSize = compress();
                    bucket.plainTextFileSize = 0;
                } else {
                    bucket.plainTextFileSize = size;
                }
            }
        }
    }

    private long compress() throws IOException {
        FileOutputStream output = null;
        FileInputStream input = null;

        try {
            output = openStream(bucket.gzPath, bucket.compressedFileSize);
            input = new FileInputStream(bucket.path);

            GZIPOutputStream gzOutput = new GZIPOutputStream(output);
            IOUtils.copy(input, gzOutput);
            gzOutput.flush();
            gzOutput.close();

            FileSystemUtils.fsync(output);

            return output.getChannel().position();
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
    }

    public void delete() {
        this.deleted = true;
    }

    @Override
    public void close() throws IOException {
        if (stream != null)
            stream.close();
    }

}
