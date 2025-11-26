package com.spinque.ciff;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.osirrc.ciff.CommonIndexFileFormat;

public class CIFFWriter implements AutoCloseable {

    private final OutputStream _out;

    private CIFFWriter(OutputStream out) {
        _out = out;
    }

    public static CIFFWriter create(Path outputFile) throws IOException {
		OutputStream out = Files.newOutputStream(outputFile);
        return new CIFFWriter(out);
    }

    public void writeHeader(CommonIndexFileFormat.Header header) throws IOException {
        header.writeDelimitedTo(_out);
    }

    public void writePostingList(CommonIndexFileFormat.PostingsList postingsList) throws IOException {
        postingsList.writeDelimitedTo(_out);
    }

    public void writeDocRecord(CommonIndexFileFormat.DocRecord docRecord) throws IOException {
        docRecord.writeDelimitedTo(_out);
    }

    @Override
    public void close() throws IOException {
        _out.close();
    }
}