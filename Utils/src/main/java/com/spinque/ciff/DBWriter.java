package com.spinque.ciff;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import org.monetdb.jdbc.MonetConnection;

public class DBWriter implements AutoCloseable {

	private final Connection _conn;
	private final String _prefix;
	public enum Table {
		DOC_TERM,
		TERMS,
		DOCS
	}

	private final Map<Table, String> _columns = Map.of(
		Table.DOC_TERM, "docID INT, termID INT, tf INT",
		Table.TERMS, "termID INT, term STRING, df INT",
		Table.DOCS, "docID INT, collectionID STRING, len INT"
	);

	private static class SimpleUploadDownloadHandler implements MonetConnection.UploadHandler, MonetConnection.DownloadHandler {

		private boolean stopUploading = false;

		@Override
		public void uploadCancelled() {
			stopUploading = true;
		}

		@Override
		public void handleUpload(MonetConnection.Upload handle, String name, boolean textMode, long linesToSkip) throws IOException {
			uploadFileData(handle, name, textMode, linesToSkip);
		}

		@Override
		public void handleDownload(MonetConnection.Download handle, String name, boolean textMode) throws IOException {
			downloadFileData(handle, name, textMode);
		}

		private void uploadFileData(MonetConnection.Upload handle, String name, boolean textMode, long linesToSkip) throws IOException {
			Path path = Path.of(name);
			if (!Files.exists(path)) {
				handle.sendError("Invalid path");
				return;
			}
			if (!Files.isReadable(path)) {
				throw new IOException("Unreadable: " + path);
			}

			uploadAsBinary(handle, path);
		}

		private void uploadAsBinary(MonetConnection.Upload handle, Path path) throws IOException {
			try (InputStream stream = Files.newInputStream(path)) {
				handle.uploadFrom(stream);
			}
		}

		private void downloadFileData(MonetConnection.Download handle, String name, boolean textMode) throws IOException {
			Path path = Path.of(name);
			if (!Files.isDirectory(path.getParent())) {
				handle.sendError("Folder must exist already");
				return;
			}

			try (OutputStream stream = Files.newOutputStream(path)) {
				handle.downloadTo(stream);
			}
		}
	}

	public DBWriter(Connection conn, String prefix) throws SQLException {
		_conn = conn;
		_prefix = prefix;

		SimpleUploadDownloadHandler handler = new SimpleUploadDownloadHandler();
		MonetConnection monetConnection = conn.unwrap(MonetConnection.class);
		monetConnection.setUploadHandler(handler);
		monetConnection.setDownloadHandler(handler);
	}

	public void write(Table t, File data) throws IOException {
		try (Statement st = _conn.createStatement()) {
			st.execute("CREATE TABLE %s(%s); ".formatted(_prefix + t.name(), _columns.get(t)));
			st.execute("COPY INTO %s FROM '%s' ON CLIENT USING DELIMITERS ',', '\\n', '\"'".formatted(_prefix + t.name(), data.getAbsolutePath()));
		} catch (SQLException e) {
			throw new IOException("Not able to load data to table %s".formatted(t.name()), e);
		}
	}

	public static DBWriter create(String connectionString, String prefix) throws SQLException {
		Connection conn = Utils.tryConnect(connectionString, new Properties());
		return new DBWriter(conn, prefix);
	}

	@Override
	public void close() throws SQLException {
		_conn.close();
	}
}
