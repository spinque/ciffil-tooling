package com.spinque.ciff.app;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.spinque.ciff.CIFFWriter;
import com.spinque.ciff.DBReader;

import picocli.CommandLine;

@CommandLine.Command(
		name = "export",
		aliases = { "generate", "build" },
		description = "Export a CIFF file from database (supported databases: MariaDB, PostgreSQL or MonetDB)"
)
public class ExportCiffFile implements Callable<Integer> {

	@CommandLine.Parameters(index = "0", arity = "1", paramLabel = "DATABASE", description = "JDBC connection string (examples: jdbc:postgres://localhost/db01, jdbc:monetdb://localhost/db01)")
	private String _connectionString;

	@CommandLine.Parameters(index = "1", arity = "1", paramLabel = "FILE", description = "Output location")
	private Path _output;

	@CommandLine.Option(names = {"--table-prefix"}, description = "Prefix for the tables in the database")
	private String _prefix = "";

	@Override
	public Integer call() throws Exception {
		try (DBReader reader = DBReader.create(_connectionString, _prefix)) {
			try (CIFFWriter writer = CIFFWriter.create(_output)) {
				reader.doExport(writer);
				return 0;
			}
		}
	}
}
