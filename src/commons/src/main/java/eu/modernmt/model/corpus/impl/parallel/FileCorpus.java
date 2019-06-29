package eu.modernmt.model.corpus.impl.parallel;

import eu.modernmt.io.*;
import eu.modernmt.lang.Language2;
import eu.modernmt.model.corpus.Corpus;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Created by davide on 10/07/15.
 */
public class FileCorpus implements Corpus {

    private FileProxy file;
    private String name;
    private Language2 language;

    private static String getNameFromFilename(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot < 0 ? filename : filename.substring(0, lastDot);
    }

    private static Language2 getLangFromFilename(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0)
            throw new IllegalArgumentException(filename);
        return Language2.fromString(filename.substring(lastDot + 1));
    }

    public FileCorpus(File file) {
        this(FileProxy.wrap(file), null, null);
    }

    public FileCorpus(File file, String name) {
        this(FileProxy.wrap(file), name, null);
    }

    public FileCorpus(File file, String name, Language2 language) {
        this(FileProxy.wrap(file), name, language);
    }

    public FileCorpus(FileProxy file) {
        this(file, null);
    }

    public FileCorpus(FileProxy file, String name) {
        this(file, name, null);
    }

    public FileCorpus(FileProxy file, String name, Language2 language) {
        this.file = file;
        this.name = (name == null ? getNameFromFilename(file.getFilename()) : name);
        this.language = (language == null ? getLangFromFilename(file.getFilename()) : language);
    }

    public FileProxy getFile() {
        return file;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Language2 getLanguage() {
        return language;
    }

    @Override
    public LineReader getContentReader() throws IOException {
        return new UnixLineReader(file.getInputStream(), UTF8Charset.get());
    }

    @Override
    public LineWriter getContentWriter(boolean append) throws IOException {
        return new UnixLineWriter(file.getOutputStream(append), UTF8Charset.get());
    }

    @Override
    public Reader getRawContentReader() throws IOException {
        return new InputStreamReader(file.getInputStream(), UTF8Charset.get());
    }

    @Override
    public String toString() {
        return name + "." + language;
    }

}
