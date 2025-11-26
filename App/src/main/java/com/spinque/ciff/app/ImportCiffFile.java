package com.spinque.ciff.app;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.spinque.ciff.CIFFReader;
import com.spinque.ciff.DBWriter;

import picocli.CommandLine;

@CommandLine.Command(
		name = "import",
		aliases = { "read" },
		description = "Import a CIFF-file into a database (supported databases: MariaDB, PostgreSQL or MonetDB)"
)
public class ImportCiffFile implements Callable<Integer> {


	@CommandLine.Parameters(index = "0", arity = "1", paramLabel = "FILE", description = "A CIFF file")
	private Path _source;

	@CommandLine.Parameters(index = "1", arity = "1", paramLabel = "DATABASE", description = "JDBC connection string (examples: jdbc:postgres://localhost/db01, jdbc:monetdb://localhost/db01)")
	private String _connectionString;

	@CommandLine.Option(names = {"--table-prefix"}, description = "Prefix for the tables")
	private String _prefix = "";

	@Override
	public Integer call() throws Exception {
		try (CIFFReader reader = CIFFReader.create(_source)) {
			try (DBWriter writer = DBWriter.create(_connectionString, _prefix)) {
				reader.parse();
				reader.write(writer);
				return 0;
			}
		}
	}
}
