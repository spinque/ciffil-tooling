package com.spinque.ciff;

import io.osirrc.ciff.CommonIndexFileFormat;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class CIFFReader implements AutoCloseable {

	private final long _numDocs;
	private final long _numPostingsLists;

	private long _termID = 0;
	private long _docID = 0;

	private final InputStream _in;

	private final File _docTerm;
	private final File _terms;
	private final File _docs;

	public static CIFFReader create(Path filename) throws IOException {
		InputStream fis = Files.newInputStream(filename);

		File docTerm = Files.createTempFile("docTerm", ".csv").toFile();
		File terms = Files.createTempFile("terms", ".csv").toFile();
		File docs = Files.createTempFile("docs", ".csv").toFile();

		docTerm.deleteOnExit();
		terms.deleteOnExit();
		docs.deleteOnExit();

		if (!(docTerm.setReadable(true, false) && terms.setReadable(true, false) && docs.setReadable(true, false)))
			throw new IOException("The database system should be able to read the file");

		try {
			CommonIndexFileFormat.Header header = io.osirrc.ciff.CommonIndexFileFormat.Header.parseDelimitedFrom(fis);
			return new CIFFReader(header, fis, docTerm, terms, docs);
		} catch (IOException e) {
			fis.close();
			throw e;
		}
	}

	public CIFFReader(CommonIndexFileFormat.Header header, InputStream in, File docTerm, File terms, File docs) throws IOException {
		_in = in;
		_numDocs = header.getNumDocs();
		_numPostingsLists = header.getNumPostingsLists();
		_docTerm = docTerm;
		_terms = terms;
		_docs = docs;
	}

	public void parse() throws IOException {
		try (
				FileOutputStream docTermFos = new FileOutputStream(_docTerm);
				FileOutputStream termsFos = new FileOutputStream(_terms);
				FileOutputStream docsFos = new FileOutputStream(_docs)
		) {
			while (_termID < _numPostingsLists) {
				CommonIndexFileFormat.PostingsList pl = CommonIndexFileFormat.PostingsList.parseDelimitedFrom(_in);
				long docID = 0;
				for (CommonIndexFileFormat.Posting p : pl.getPostingsList()) {
					docID += p.getDocid();
					int tf = p.getTf();
					docTermFos.write("%d,%d,%d\n".formatted(docID, _termID, tf).getBytes());
				}
				String term = pl.getTerm();
				long df = pl.getDf();
				if (term.equals("null"))
					term = "\"null\"";
				termsFos.write("%d,%s,%d\n".formatted(_termID, StringEscapeUtils.escapeCsv(term), df).getBytes());
				_termID += 1;
			}
			// parse docs
			while (_docID < _numDocs) {
				CommonIndexFileFormat.DocRecord doc = CommonIndexFileFormat.DocRecord.parseDelimitedFrom(_in);
				String collectionID = doc.getCollectionDocid();
				long docID = doc.getDocid();
				int length = doc.getDoclength();
				docsFos.write("%d,%s,%d\n".formatted(docID, StringEscapeUtils.escapeCsv(collectionID), length).getBytes());
				_docID += 1;
			}
		}
	}

	public void write(DBWriter writer) throws IOException {
		writeTo(writer, DBWriter.Table.DOC_TERM, _docTerm);
		writeTo(writer, DBWriter.Table.TERMS, _terms);
		writeTo(writer, DBWriter.Table.DOCS, _docs);
	}

	public void writeTo(DBWriter writer, DBWriter.Table table, File file) throws IOException {
		writer.write(table, file);
	}

	public long getDocID() {
		return _docID;
	}

	public long getTermID() {
		return _termID;
	}

	@Override
	public void close() throws Exception {
		_in.close();
	}
}
