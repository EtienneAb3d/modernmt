package eu.modernmt.cluster.db;

import eu.modernmt.cluster.EmbeddedService;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.engine.FileConst;
import eu.modernmt.io.DefaultCharset;
import eu.modernmt.io.NetworkUtils;
import eu.modernmt.io.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 07/04/17.
 */
public class EmbeddedCassandra extends EmbeddedService {

    public static EmbeddedCassandra start(Engine engine, int port) throws BootstrapException {
        try {
            EmbeddedCassandra instance = new EmbeddedCassandra(engine);
            instance.start(port);

            return instance;
        } catch (IOException e) {
            throw new BootstrapException(e);
        }
    }

    private final String clusterName;
    private final File db;
    private final File runtime;
    private final File bin;
    private final File configTemplate;
    private final File logFile;

    private Process process;

    private EmbeddedCassandra(Engine engine) throws IOException {
        this.clusterName = "ModernMT - " + engine.getName();
        this.db = Paths.join(engine.getModelsPath(), "db", "cassandra");
        this.runtime = engine.getRuntimeFolder("cassandra", true);
        this.logFile = engine.getLogFile("embedded-cassandra.log");

        File cassandraHome = Paths.join(FileConst.getVendorPath(), "cassandra-3.10");

        this.bin = Paths.join(cassandraHome, "bin", "cassandra");
        this.configTemplate = Paths.join(cassandraHome, "conf", "cassandra.yaml");
    }

    public File getLogFile() {
        return logFile;
    }

    private void start(int port) throws IOException {
        if (!NetworkUtils.isAvailable(port))
            throw new IOException("Port " + port + " is already in use by another process");

        if (!this.db.isDirectory())
            FileUtils.forceMkdir(this.db);

        FileUtils.deleteQuietly(this.logFile);
        FileUtils.touch(this.logFile);

        File config = this.createConfig(port);

        ProcessBuilder builder = new ProcessBuilder(this.bin.getAbsolutePath(),
                "-R", "-Dcassandra.config=file:///" + config, "-f");
        builder.environment()
                .put("CASSANDRA_JMX_PORT", Integer.toString(NetworkUtils.getAvailablePort()));
        builder.redirectError(ProcessBuilder.Redirect.appendTo(this.logFile));
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(this.logFile));

        this.process = builder.start();

        boolean success = false;
        try {
            waitForStartupCompleted();
            success = true;
        } finally {
            if (!success)
                this.process.destroyForcibly();
        }

        this.subprocesses = Collections.singletonList(process);
    }

    private File createConfig(int port) throws IOException {
        File config = new File(this.runtime, "cassandra.yaml");

        HashMap<String, String> customConfigurations = new HashMap<>(16);
        customConfigurations.put("cluster_name", this.clusterName);

        // port used for DB communication
        customConfigurations.put("native_transport_port", Integer.toString(port));

        // auto snapshot on drop can cause timeouts
        customConfigurations.put("auto_snapshot", "false");

        // directories to save data into
        customConfigurations.put("commitlog_directory", new File(this.db, "commitlog").getAbsolutePath());
        customConfigurations.put("data_file_directories", "\n     - " + new File(this.db, "data").getAbsolutePath());
        customConfigurations.put("saved_caches_directory", new File(this.db, "saved_caches").getAbsolutePath());

        // ports that we do not use
        customConfigurations.put("storage_port", Integer.toString(NetworkUtils.getAvailablePort()));
        customConfigurations.put("ssl_storage_port", Integer.toString(NetworkUtils.getAvailablePort()));
        customConfigurations.put("rpc_port", Integer.toString(NetworkUtils.getAvailablePort()));

        // accept remote requests
        customConfigurations.put("rpc_address", "0.0.0.0");
        customConfigurations.put("broadcast_rpc_address", "1.2.3.4");

        Writer out = null;

        try {
            out = new OutputStreamWriter(new FileOutputStream(config, false), DefaultCharset.get());

            Iterator<String> input = FileUtils.lineIterator(this.configTemplate, DefaultCharset.get().name());
            while (input.hasNext()) {
                String line = input.next();
                String key = extractKey(line, customConfigurations);

                if (key != null) {
                    String value = customConfigurations.get(key);
                    customConfigurations.remove(key);

                    line = key + ": " + value + '\n';
                }

                out.write(line);
                out.write('\n');
            }

            for (Map.Entry<String, String> line : customConfigurations.entrySet())
                out.write(line.getKey() + ": " + line.getValue() + '\n');
        } finally {
            IOUtils.closeQuietly(out);
        }

        return config;
    }

    private static String extractKey(String line, HashMap<String, String> config) {
        for (String key : config.keySet()) {
            if (line.contains(key))
                return key;
        }

        return null;
    }

    private void waitForStartupCompleted() throws IOException {
        for (int i = 0; i < 100; i++) {
            if (!this.process.isAlive())
                throw new IOException("Unable to start Cassandra process, more details here: " + this.logFile.getAbsolutePath());

            LineIterator lines = FileUtils.lineIterator(this.logFile, DefaultCharset.get().name());

            while (lines.hasNext()) {
                String line = lines.next();

                if (line.contains("Starting listening for CQL clients"))
                    return;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new IOException("Unexpected interruption", e);
            }
        }

        throw new IOException("Cassandra process startup timeout, more details here: " + this.logFile.getAbsolutePath());
    }

    @Override
    public void shutdown() {
        this.kill(process, 5, TimeUnit.SECONDS);
    }

}
