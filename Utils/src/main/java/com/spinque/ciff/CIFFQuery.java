package com.spinque.ciff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CIFFQuery implements AutoCloseable {

	private static final String QUERY_TERMS_QUERY_TEMPLATE = """
		SELECT termid
		FROM %PREFIX%terms
		WHERE term IN (%TERMS%);
	""";

	private static final String BM25_QUERY_TEMPLATE = """
		WITH qterms AS (
			SELECT termid, docid, tf
			FROM %PREFIX%doc_term
			WHERE termid IN (%TERM_IDS%)
		), subscores AS (
			SELECT %PREFIX%docs.collectionid, %PREFIX%docs.docid, len, term_tf.termid, tf, df, (log((%COLLECTION_SIZE%-df+0.5)/(df+0.5))*((tf*(1.2+1)/(tf+1.2*(1-0.75+0.75*(len/%AVG_DOC_LEN%)))))) AS subscore
		  	FROM (
		  		SELECT termid, docid, tf
		  		FROM qterms
		  	) AS term_tf
		  	JOIN (
		  		SELECT docid
		  		FROM qterms
		  		GROUP BY docid
		  	) AS cdocs
		  	ON term_tf.docid = cdocs.docid
		  	JOIN %PREFIX%docs
		  	ON term_tf.docid=%PREFIX%docs.docid
		 	JOIN %PREFIX%terms
		 	ON term_tf.termid=%PREFIX%terms.termid
		)
		SELECT scores.collectionID, score
		FROM (
			SELECT collectionID, sum(subscore) AS score
		  	FROM subscores
		  	GROUP BY collectionID
		) AS scores
		JOIN %PREFIX%docs
		ON scores.collectionID=%PREFIX%docs.collectionID
		ORDER BY score DESC
		LIMIT %TOPN%;
		""";

	private final Connection _conn;
	private final String _prefix;
	private final double _averageDocumentLength;
	private final int _collectionSize;

	public CIFFQuery(Connection conn, String prefix, double averageDocumentLength, int collecionSize) {
		_prefix = prefix;
		_conn = conn;
		_averageDocumentLength = averageDocumentLength;
		_collectionSize = collecionSize;
	}

	public record Result (String docID, double score){}

	public static CIFFQuery create(String connectionString, String prefix) throws SQLException, IOException {
		Connection conn = Utils.tryConnect(connectionString, new Properties());
		double avgDocumentLength = queryAverageDocumentLength(conn, prefix);
		int collectionSize = queryCollectionSize(conn, prefix);
		return new CIFFQuery(conn, prefix, avgDocumentLength, collectionSize);
	}

	public List<Result> doQuery(String query, int topN) throws SQLException {
		try (Statement st = _conn.createStatement()) {
			String queryList = splitWord(query);
			List<String> queryTermIDS = new ArrayList<>();

			String queryTermIdsQuery = QUERY_TERMS_QUERY_TEMPLATE
					.replace("%PREFIX%", _prefix)
					.replace("%TERMS%", queryList);
			try (ResultSet rs = st.executeQuery(queryTermIdsQuery)) {
				while (rs.next()) {
					queryTermIDS.add("" + rs.getInt("termid"));
				}
			}

			String sqlQuery = BM25_QUERY_TEMPLATE
					.replace("%TERM_IDS%", String.join(",", queryTermIDS))
					.replace("%PREFIX%", _prefix)
					.replace("%COLLECTION_SIZE%", "" + _collectionSize)
					.replace("%AVG_DOC_LEN%", "" + _averageDocumentLength)
					.replace("%TOPN%", "" + topN);

			try (ResultSet rs = st.executeQuery(sqlQuery)) {
				List<Result> result = new ArrayList<>();
				while (rs.next()) {
					String docID = rs.getString("collectionID");
					double score = rs.getDouble("score");
					result.add(new Result(docID, score));
				}
				return result;
			}
		}
	}

	private static double queryAverageDocumentLength(Connection conn, String prefix) throws SQLException, IOException {
		String AVG_DOC_LENGTH_QUERY = """
				SELECT avg(len) AS avgdoclen
				FROM %PREFIX%docs;
			""".replace("%PREFIX%", prefix);
		double avgDocLength;
		try (Statement st = conn.createStatement()) {
			try (ResultSet rs = st.executeQuery(AVG_DOC_LENGTH_QUERY)) {
				if (rs.next()) {
					avgDocLength = rs.getDouble("avgdoclen");
				} else {
					throw new IOException("Not possible to determine the average document length");
				}
			}
		}
		return avgDocLength;
	}

	private static int queryCollectionSize(Connection conn, String prefix) throws SQLException, IOException {
		String COLLECTION_SIZE_QUERY = """
				SELECT count(docid) AS collectionsize FROM %PREFIX%docs;
			""".replace("%PREFIX%", prefix);
		int collectionSize;
		try (Statement st = conn.createStatement()) {
			try (ResultSet rs = st.executeQuery(COLLECTION_SIZE_QUERY)) {
				if (rs.next()) {
					collectionSize = rs.getInt("collectionsize");
				} else {
					throw new IOException("Not possible to determine the collection size");
				}
			}
		}
		return collectionSize;
	}

	private String splitWord(String query) {
		List<String> parts = new ArrayList<>();
		for (String part : query.split("\\s+")) {
			if (!part.isBlank())
				parts.add("'" + part + "'");
		}
		return String.join(",", parts);
	}

	@Override
	public void close() throws Exception {
		_conn.close();
	}
}
