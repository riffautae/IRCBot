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

import org.pircbotx.hooks.events.MessageEvent;
import java.sql.*;
import java.util.Date;

public class ShoutHandler implements Runnable {
	// Variables
	private MessageEvent event = null;
	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;
	private String randomQuote = null;
	private boolean isRandomShout = false;
	
	// We need this variable to be accessible from other threads, so we make it static and volatile
	private static volatile String lastQuote = "";
	private static volatile String lastSubmitter = "";
	private static volatile String lastReadableDate = "";
	
	// Method that executes upon start of thread
	public void run() {
		try {
			// Connect to the database
			connect();
			// If the message passed is NOT a !who command
			if(isRandomShout) {
				// Get a random quote from the database (if possible). Send it to the channel.
				// If the quote does not exist in the database, add it!
				if((randomQuote = getRandomQuote()) != null) {
					event.getBot().sendMessage(event.getChannel(), randomQuote);
				}
				if(!doesQuoteExist()) addNewQuote();
			} else {
				// We're dealing with a !who command - respond to the user with the information about the quote.
				event.respond(getQuoteInfo(event.getMessage().substring(5).replaceAll("^\\s+", "").replaceAll("\\s+$", "")));
			}
			// Disconnect from the database
			disconnect();
		} catch (Exception ex) {
			EventLogger.Log(EventLogger.LOG_ERROR, ex.getMessage());
			ex.printStackTrace();
			return;
		}
	}
	
	// Constructor for the class
	public ShoutHandler(MessageEvent event) {
		this.event = event;
	}
	
	// Overloadable constructor, used when a shout needs to be processed
	public ShoutHandler(MessageEvent event, boolean isRandomShout) {
		this.event = event;
		this.isRandomShout = isRandomShout;
	}
	
	// Method to connect to the database
	private void connect() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		connect = DriverManager.getConnection("jdbc:mysql://localhost/irc_shouts?user=ircbot&password=milkircbot");
		statement = connect.createStatement();
	}
	
	// Method to clean up/disconnect connections to the database
	private void disconnect() throws SQLException {
		if(resultSet != null) resultSet.close();
		if(statement != null) statement.close();
		if(connect != null) connect.close();
	}
	
	// Method to retrieve a random quote from the database
	private String getRandomQuote() throws SQLException {
		// We use prepared statements to sanitize input from the user
		// Specifying the channel allows different channels to have their own list of quotes available
		preparedStatement = connect.prepareStatement("SELECT * FROM Quotes WHERE Channel = ? ORDER BY RAND() LIMIT 1");
		preparedStatement.setString(1, event.getChannel().getName());
		// Execute our query against the database
		resultSet = preparedStatement.executeQuery();
		if(resultSet.next()) {
			// Strings are not thread-safe, so ensure synchronization on our shared variables to prevent thread
			// interference and memory consistency issues
			synchronized(lastQuote) {
				lastQuote = resultSet.getString("Quote");
			}
			synchronized(lastSubmitter) {
				lastSubmitter = resultSet.getString("Nick");
			}
			synchronized(lastReadableDate) {
				lastReadableDate = toReadableTime((Date)resultSet.getTimestamp("Date"));
			}
			// Return the random quote
			return resultSet.getString("Quote");
		} else {
			// The database query returned nothing, so return null
			return null;
		}
	}
	
	// Method to check if a quote exists
	private boolean doesQuoteExist() throws SQLException {
		// Again, prepared statements to sanitize input
		preparedStatement = connect.prepareStatement("SELECT * FROM Quotes WHERE Quote = ? AND Channel = ?");
		preparedStatement.setString(1, event.getMessage());
		preparedStatement.setString(2, event.getChannel().getName());
		resultSet = preparedStatement.executeQuery();
		if(resultSet.next()) {
			return true;
		} else {
			return false;
		}
	}
	
	// Method to handle !who requests from users, returns submitter and timestamp
	private String getQuoteInfo(String quote) throws SQLException {
		// Variable to track whether the user requested the last quote from the bot
		// If the user wants the last quote, prevent hitting the database twice and simply retrieve details from memory
		if(quote.equals("last")) {
			// On startup there is no previous quote, so return as such if a user attempts a !who last
			if(lastQuote == null || lastQuote == "") return "No previous quote.";
			// Provide context if the !who last command was used, but trim it if the quote is longer than 10 characters (use 60% of the quote instead)
			if(quote.length() < 11) {
				return lastSubmitter + " shouted \"" + lastQuote + "\" " + lastReadableDate + ".";
			} else {
				return lastSubmitter + " shouted \"" + lastQuote.substring(0, (int)(lastQuote.length() * 0.6)) + "...\" " + lastReadableDate + ".";
			}
		}
		// We need 2 queries to pull details about the database (for now), sadly.
		else if(quote.equals("list")) {
			// First query to pull the total number of quotes
			preparedStatement = connect.prepareStatement("SELECT COUNT(*) FROM Quotes");
			resultSet = preparedStatement.executeQuery();
			if(resultSet.next()) {
				int count = resultSet.getInt("COUNT(*)");
				// and the second to pull the most active shouter.
				preparedStatement = connect.prepareStatement("SELECT COUNT(Nick), Nick FROM Quotes GROUP BY Nick ORDER BY COUNT(Nick) DESC LIMIT 1");
				resultSet = preparedStatement.executeQuery();
				if(resultSet.next()) {
					// If both of these execute successfully (which they always should) then return the details the user asked for
					return "I have " + count + " quotes in my database. The most active shouter is " + resultSet.getString("Nick") + " with " + resultSet.getInt("COUNT(Nick)") + ".";
				}
			}
		}
		// You should know why by now.
		preparedStatement = connect.prepareStatement("SELECT * FROM Quotes WHERE Quote = ? AND Channel = ?");
		preparedStatement.setString(1, quote);
		preparedStatement.setString(2, event.getChannel().getName());
		resultSet = preparedStatement.executeQuery();
		if(resultSet.next()) {
			// Tease the user if it's their own quote
			if(resultSet.getString("Nick").equals(event.getUser().getNick())) return "don't you remember? YOU submitted this! Put down the bong!";
			return resultSet.getString("Nick") + " shouted this " + toReadableTime((Date)resultSet.getTimestamp("Date")) + ".";
		} else {
			return "Quote not found.";
		}
	}
	
	// Method to add a new quote to the database.
	private void addNewQuote() throws SQLException {
		java.util.Date dt = new java.util.Date();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		preparedStatement = connect.prepareStatement("INSERT INTO Quotes(Nick, Date, Channel, Quote) VALUES (?, ?, ?, ?)");
		preparedStatement.setString(1, event.getUser().getNick());
		preparedStatement.setString(2, sdf.format(dt));
		preparedStatement.setString(3, event.getChannel().getName());
		preparedStatement.setString(4, event.getMessage());
		preparedStatement.executeUpdate();
	}
	
	// Method to convert a date into a more readable time format.
	private String toReadableTime(Date date) {
		// Calculate the difference in seconds between the quote's submission and now
		long diffInSeconds = (new Date().getTime() - date.getTime()) / 1000;

		// Calculate the appropriate day/hour/minute/seconds ago values and insert them into a long array
	    long diff[] = new long[] { 0, 0, 0, 0 };
	    diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
	    diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
	    diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
	    diff[0] = (diffInSeconds = (diffInSeconds / 24));
	    
	    // Build the readable format string
	    if(diff[0] != 0) return String.format("about %d day%s ago", diff[0], diff[0] > 1 ? "s" : "");
	    if(diff[1] != 0) return String.format("about %s%s hour%s ago", diff[1] > 1 ? "" : "an", diff[1] > 1 ? String.valueOf(diff[1]) : "", diff[1] > 1 ? "s" : "");
	    if(diff[2] != 0) return String.format("about %d minute%s ago", diff[2], diff[2] > 1 ? "s" : "");
	    if(diff[3] != 0) return "just a moment ago";
	    else return "an unknown time ago";
	}
}
