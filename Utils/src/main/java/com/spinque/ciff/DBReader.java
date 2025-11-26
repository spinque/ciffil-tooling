package com.spinque.ciff;

import io.osirrc.ciff.CommonIndexFileFormat;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;

public class DBReader implements AutoCloseable {

    private final Connection _conn;
    private final Instant _startTime;
    private final String _prefix;

    private static final int VERSION = 1;

    public DBReader(Connection conn, String prefix) {
        _conn = conn;
        _prefix = prefix;
        _startTime = Instant.now();
    }

    public static DBReader create(String connectionString, String prefix) throws SQLException {
        Connection conn = Utils.tryConnect(connectionString, new Properties());
        return new DBReader(conn, prefix);
    }

	public static DBReader create(String connectionString, Properties p, String prefix) throws SQLException {
		Connection conn = Utils.tryConnect(connectionString, p);
		return new DBReader(conn, prefix);
	}

    public void doExport(CIFFWriter writer) throws SQLException {
        try (Statement st = _conn.createStatement()) {
            st.execute("START TRANSACTION;");
            makeView(st);
            exportHeader(writer, st);
            exportPostings(writer, st);
            exportDocuments(writer, st);
            st.execute("ROLLBACK;");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void makeView(Statement st) throws SQLException {
        String view = """
			CREATE VIEW %PREFIX%ciff AS
			SELECT t.term as token, d.docid as rowid, d.collectionid, count(dt.termid) as tf
			FROM %PREFIX%doc_term as dt, %PREFIX%docs AS d, %PREFIX%terms AS t
			WHERE dt.docid=d.docid
			AND dt.termid=t.termid
			GROUP BY t.term, d.docid, d.collectionid
			ORDER BY term,docid;
		""".replace("%PREFIX%", _prefix);
        st.execute(view);
    }

    private int getNumberOfPostingLists(Statement st) throws SQLException {
        long numPostingsLists = Utils.getSingleInteger(st, makeQuery("SELECT COUNT(DISTINCT token) FROM %TABLE%;"), 0);
        if (numPostingsLists == 0)
            throw new IllegalArgumentException("No data found to export, an empty CIFF index will be published.");
        return (int) numPostingsLists;
    }


    /** How many documents are in the collection **/
    private int getNumberOfDocumentsInCollection(Statement st) throws SQLException {
        return (int) Utils.getSingleInteger(st, makeQuery("SELECT MAX(rowID) FROM %TABLE%;"), 0);
    }

    /** How many documents are written to the index **/
    private int getNumberOfDocumentsExported(Statement st) throws SQLException {
        return (int) Utils.getSingleInteger(st, makeQuery("SELECT COUNT(DISTINCT rowID) FROM %TABLE%;"), 0);
    }

    private long getNumberOfTokens(Statement st) throws SQLException {
        return Utils.getSingleInteger(st, makeQuery("SELECT SUM(tf) FROM %TABLE%;"), 0);
    }

    private double getAverageNumberOfTokens(Statement st) throws SQLException {
        return Utils.getSingleDouble(st, makeQuery("""
			SELECT AVG(t.ntokens) AS average
		    FROM (
		 	    SELECT collectionID, SUM(tf) AS ntokens
			    FROM %TABLE%
			    GROUP BY collectionID
		    ) AS t;
		"""), 0d);
    }

    private void exportHeader(CIFFWriter writer, Statement st) throws SQLException, IOException {
        CommonIndexFileFormat.Header.Builder builder = CommonIndexFileFormat.Header.newBuilder();
        builder.setVersion(VERSION);

        // both are for Spinque the same, we do not support only subset of tokens

        // If the number of tokens or documents are zero, we only warn once.
        int nPostings = getNumberOfPostingLists(st);
        builder.setNumPostingsLists(nPostings);
        builder.setTotalPostingsLists(nPostings);

        // The number of unique row_ids (documents to be exported: empty docs (e.g. after tokenizing) do not get an entry in the export)
        builder.setNumDocs(getNumberOfDocumentsExported(st));
        // the max row_id (documents in the collection, including empty documents )
        builder.setTotalDocs(getNumberOfDocumentsInCollection(st));

        builder.setTotalTermsInCollection(getNumberOfTokens(st));
        builder.setAverageDoclength(getAverageNumberOfTokens(st));

        CommonIndexFileFormat.Header header = builder.build();
        writer.writeHeader(header);
    }

    void exportPostings(CIFFWriter writer, Statement st) throws SQLException, IOException {
        String query = makeQuery("""
			SELECT token, rowID, tf
			FROM %TABLE%
			ORDER BY token, rowID;
		""");
        try (ResultSet rs = st.executeQuery(query)) {
            int tokenCol = rs.findColumn("token");
            int rowIDCol = rs.findColumn("rowID");
            int tfCol = rs.findColumn("tf");

            CommonIndexFileFormat.PostingsList.Builder plBuilder = CommonIndexFileFormat.PostingsList.newBuilder();

            if (!rs.first())
                return;

            String currentToken = rs.getString(tokenCol);
            int currentDocID = rs.getInt(rowIDCol);
            int currentDF = 1; // document frequency = number of documents the token appears in
            int currentCF = rs.getInt(tfCol);  // collection frequency = sum of TF of token over all documents

            plBuilder.addPostings(CommonIndexFileFormat.Posting.newBuilder().setDocid(currentDocID).setTf(currentCF).build());

            while (rs.next()) {
                String token = rs.getString(tokenCol);
                int tf = rs.getInt(tfCol);
                int docID = rs.getInt(rowIDCol);

                if (Objects.equals(token, currentToken)) {
                    // if it is the same term we update the posting list,
                    int delta = docID - currentDocID; // because of delta encoding we store the difference between the ID's
                    currentDocID = docID;
                    currentDF += 1;
                    currentCF += tf;

                    plBuilder.addPostings(CommonIndexFileFormat.Posting.newBuilder().setDocid(delta).setTf(tf).build());
                } else {
                    // if it is a different term we write the old posting list, and create a new one for the new term
                    CommonIndexFileFormat.PostingsList pl = plBuilder.setTerm(currentToken).setCf(currentCF).setDf(currentDF).build();
                    writer.writePostingList(pl);

                    currentToken = token;
                    currentDocID = docID;
                    currentDF = 1;
                    currentCF = tf;

                    plBuilder = CommonIndexFileFormat.PostingsList.newBuilder();
                    plBuilder.addPostings(CommonIndexFileFormat.Posting.newBuilder().setDocid(currentDocID).setTf(tf).build());
                }
            }
            // no new tokens, so flush the last posting list
            writer.writePostingList(plBuilder.setTerm(currentToken).setCf(currentCF).setDf(currentDF).build());
        }
    }

    private void exportDocuments(CIFFWriter writer, Statement st) throws SQLException, IOException {
        String query = makeQuery("""
			SELECT rowID, collectionID, SUM(tf) AS length
			FROM %TABLE%
			GROUP BY collectionID, rowID
			ORDER BY rowID;
		""");
        // doc length is the number of tokens in the document
        try (ResultSet rs = st.executeQuery(query)) {
            int rowIDCol = rs.findColumn("rowID");
            int collectionIDCol = rs.findColumn("collectionID");
            int tfIDCol = rs.findColumn("length");
            while (rs.next()) {
                int docID = rs.getInt(rowIDCol);
                String collectionID = rs.getString(collectionIDCol);
                int tf = rs.getInt(tfIDCol);

                CommonIndexFileFormat.DocRecord.Builder record = CommonIndexFileFormat.DocRecord.newBuilder();
                writer.writeDocRecord(record.setDocid(docID).setCollectionDocid(collectionID).setDoclength(tf).build());
            }
        }
    }

    /* Helper */
    private String makeQuery(String query) {
        return query.replace("%TABLE%", _prefix + "ciff");
    }

    @Override
    public void close() throws SQLException {
        _conn.close();
    }
}