package com.spinque.ciff.app;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.spinque.ciff.CIFFQuery;
import com.spinque.ciff.CIFFWriter;
import com.spinque.ciff.DBReader;

import picocli.CommandLine;

@CommandLine.Command(
		name = "query",
		aliases = { "search" },
		description = "Search an imported CIFF file in a database"
)
public class QueryCiff implements Callable<Integer> {

	@CommandLine.Parameters(index = "0", arity = "1", paramLabel = "DATABASE", description = "JDBC connection string (examples: jdbc:postgres://localhost/db01, jdbc:monetdb://localhost/db01)")
	private String _connectionString;

	@CommandLine.Parameters(index = "1", arity = "1", paramLabel = "QUERY", description = "The needle")
	private String _query;

	@CommandLine.Option(names = {"--table-prefix"}, description = "Prefix for the tables in the database")
	private String _prefix = "";

	@CommandLine.Option(names = {"-n"}, description = "How many documents to return")
	private int _n = 100;

	@Override
	public Integer call() throws Exception {
		try (CIFFQuery index = CIFFQuery.create(_connectionString, _prefix)) {
			/* build view */
			for (CIFFQuery.Result entry : index.doQuery(_query, _n)) {
				System.out.printf(" [%.03f] : %s%n", entry.score(), entry.docID());
			}
		}
		return 0;
	}
}
