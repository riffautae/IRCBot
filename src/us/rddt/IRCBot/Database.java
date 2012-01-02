/*
 * This file is part of IRCBot.
 * Copyright (c) 2011 Ryan Morrison
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions, and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions, and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of the author of this software nor the name of
 *  contributors to this software may be used to endorse or promote products
 *  derived from this software without specific prior written consent.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

package us.rddt.IRCBot;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*
 * @author Ryan Morrison
 */
public class Database {
	/*
	 * Class variables.
	 */
	private Connection connection = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;
	private Statement statement = null;

	/*
	 * Class constructor.
	 */
	public Database() {
	}

	/*
	 * Connects to the database as specified in IRCBot.properties.
	 * @throws SQLException if a SQL exception occurs
	 * @throws ClassNotFoundException if the JDBC driver cannot be loaded
	 * @throws IOException if the properties file cannot be loaded
	 */
	public void connect() throws SQLException, ClassNotFoundException, IOException {
		if(Configuration.getDatabaseDriver().equalsIgnoreCase("mysql")) {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://" + Configuration.getMySQLServer() + "/" + Configuration.getMySQLDatabase() + "?user=" + Configuration.getMySQLUser() + "&password=" + Configuration.getMySQLPassword());
		} else if(Configuration.getDatabaseDriver().equalsIgnoreCase("sqlite")) {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:" + Configuration.getSQLiteDatabase() + ".db");
		} else {
			throw new SQLException("Invalid SQL configuration in properties file");
		}
		statement = connection.createStatement();
	}

	/*
	 * Cleans up any potential connections left behind when the database was accessed.
	 * @throws SQLException if a SQL exception occurs
	 */
	public void disconnect() throws SQLException {
		if(resultSet != null) resultSet.close();
		if(statement != null) statement.close();
		if(connection != null) connection.close();
	}

	/*
	 * Gets the database connection.
	 * @return the database connection
	 */
	public Connection getConnection() {
		return connection;
	}

	/*
	 * Gets the prepared statement for use with the database
	 * @return the prepared statement for use with the database
	 */
	public PreparedStatement getPreparedStatement() {
		return preparedStatement;
	}

	/*
	 * Gets the result set for a query, or null if the query has not executed
	 * @return the result set for a processed query
	 */
	public ResultSet getResultSet() {
		return resultSet;
	}

	/*
	 * Gets the connection's statement.
	 * @return the connection's statement
	 */
	public Statement getStatement() {
		return statement;
	}
}
