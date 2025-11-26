package com.spinque.ciff;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;

public class Utils {

	public static long getSingleInteger(Statement st, String query, long defaultValue) throws SQLException {
		try (ResultSet rs = st.executeQuery(query)) {
			if (!rs.next())
				return defaultValue;
			return rs.getLong(1);
		}
	}
	public static double getSingleDouble(Statement st, String query, double defaultValue) throws SQLException {
		try (ResultSet rs = st.executeQuery(query)) {
			if (!rs.next())
				return defaultValue;
			return rs.getDouble(1);
		}
	}


	public static Connection tryConnect(final String connectionString, Properties p) throws SQLException {
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver d = drivers.nextElement();
			if (d.acceptsURL(connectionString))
				return d.connect(connectionString, p);
		}
		throw new SQLException("no driver available for " + connectionString);
	}

}
