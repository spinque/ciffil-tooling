package com.spinque.ciff;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestWriteAndRead {

	// MonetDB with database ciff should exist for the test to work.
	static final String DB_CONNECTION_STRING = "jdbc:monetdb://localhost/ciff";
	static final String USERNAME = "monetdb";
	static final String PASSWORD = "monetdb";

	static final String CIFF_TABLE = "_" + String.valueOf(UUID.randomUUID()).replace('-', '_');

	static Properties _properties;

	static File _tmp_file;

	@BeforeAll
	public static void prepare() throws SQLException, IOException {
		_tmp_file = File.createTempFile("tmp", ".ciff");
		_properties = new Properties();
		_properties.putAll(Map.of("user", USERNAME, "password", PASSWORD));
		try (Connection connection = Utils.tryConnect(DB_CONNECTION_STRING, _properties)) {
			Statement statement = connection.createStatement();
			statement.execute("CREATE TABLE %s (token VARCHAR NOT NULL, rowID int NOT NULL, collectionID VARCHAR NOT NULL, tf int NOT NULL);".formatted(CIFF_TABLE));
			statement.execute("INSERT INTO %s VALUES('hi', 0, 'doc1', 2);".formatted(CIFF_TABLE));
			statement.execute("INSERT INTO %s VALUES('hi', 1, 'doc2', 3);".formatted(CIFF_TABLE));
			statement.execute("INSERT INTO %s VALUES('hello', 0, 'doc1', 7);".formatted(CIFF_TABLE));
			statement.execute("INSERT INTO %s VALUES('hello', 1, 'doc2', 9);".formatted(CIFF_TABLE));
		}
	}

	@AfterAll
	public static void cleanup() throws SQLException {
		try (Connection connection = Utils.tryConnect(DB_CONNECTION_STRING, _properties)) {
			Statement statement = connection.createStatement();
			statement.execute("DROP TABLE %s;".formatted(CIFF_TABLE));
		}
	}

    @Test
	@Order(1)
    public void table_created_correctly() throws SQLException {
		try (Connection connection = Utils.tryConnect(DB_CONNECTION_STRING, _properties)) {
			Statement statement = connection.createStatement();
			try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS c FROM %s".formatted(CIFF_TABLE))) {
				Assertions.assertTrue(rs.next());
				Assertions.assertEquals(4, rs.getInt("c"));
				Assertions.assertFalse(rs.next());
			}
		}
    }

	@Test
	@Order(2)
	public void write() throws SQLException, IOException {
		// Test if data was written to the ciff file.
		long size_before = Files.size(_tmp_file.toPath());
		try (DBReader reader = DBReader.create(DB_CONNECTION_STRING, _properties, "")) {
			try (CIFFWriter writer = CIFFWriter.create(_tmp_file.toPath())) {
				reader.doExport(writer);
			}
		}
		long size_after = Files.size(_tmp_file.toPath());
		Assertions.assertTrue(size_after > size_before);
	}

	@Test
	@Order(3)
	public void read() throws Exception {
		try (CIFFReader reader = CIFFReader.create(_tmp_file.toPath())) {
			reader.parse();
			Assertions.assertEquals(2, reader.getDocID());
			Assertions.assertEquals(2, reader.getTermID());
		}
	}
}
