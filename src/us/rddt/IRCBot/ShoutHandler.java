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

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class ShoutHandler implements Runnable {
	// Variables
	private MessageEvent<PircBotX> event = null;
	private String randomQuote = null;
	private boolean isRandomShout = false;

	private Database database;

	// We need this variable to be accessible from other threads, so we make it static and volatile
	private static volatile String lastQuote = "";
	private static volatile String lastSubmitter = "";
	private static volatile String lastReadableDate = "";

	// Method that executes upon start of thread
	public void run() {
		try {
			// Connect to the database
			database = new Database();
			database.connect();
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
				try {
					event.respond(getQuoteInfo(event.getMessage().substring(5).replaceAll("^\\s+", "").replaceAll("\\s+$", "")));
				} catch (IndexOutOfBoundsException ex) {
					return;
				}
			}
			// Disconnect from the database
			database.disconnect();
		} catch (Exception ex) {
			IRCUtils.Log(IRCUtils.LOG_ERROR, ex.getMessage());
			ex.printStackTrace();
			return;
		}
	}

	// Constructor for the class
	public ShoutHandler(MessageEvent<PircBotX> event) {
		this.event = event;
	}

	// Overloadable constructor, used when a shout needs to be processed
	public ShoutHandler(MessageEvent<PircBotX> event, boolean isRandomShout) {
		this.event = event;
		this.isRandomShout = isRandomShout;
	}

	// Method to retrieve a random quote from the database
	private String getRandomQuote() throws SQLException {
		// We use prepared statements to sanitize input from the user
		// Specifying the channel allows different channels to have their own list of quotes available
		PreparedStatement statement= database.getConnection().prepareStatement("SELECT * FROM Quotes WHERE Channel = ? ORDER BY RAND() LIMIT 1");
		statement.setString(1, event.getChannel().getName());
		// Execute our query against the database
		ResultSet resultSet = statement.executeQuery();
		if(resultSet.next()) {
			// Save the last quote to prevent an extra DB hit on !who last
			synchronized(lastQuote) {
				lastQuote = resultSet.getString("Quote");
			}
			synchronized(lastSubmitter) {
				lastSubmitter = resultSet.getString("Nick");
			}
			synchronized(lastReadableDate) {
				lastReadableDate = IRCUtils.toReadableTime((Date)resultSet.getTimestamp("Date"), false);
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
		PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM Quotes WHERE Quote = ? AND Channel = ?");
		statement.setString(1, event.getMessage());
		statement.setString(2, event.getChannel().getName());
		ResultSet resultSet = statement.executeQuery();
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
			// Tease the user if it's their own quote
			if(lastSubmitter.equals(event.getUser().getNick())) return "don't you remember? YOU submitted this! Put down the bong!";
			// Provide context if the !who last command was used, but trim it if the quote is longer than 10 characters (use 60% of the quote instead)
			if(quote.length() < 11) {
				return lastSubmitter + " shouted \"" + lastQuote + "\" about " + lastReadableDate + " ago.";
			} else {
				return lastSubmitter + " shouted \"" + lastQuote.substring(0, (int)(lastQuote.length() * 0.6)) + "...\" " + lastReadableDate + ".";
			}
		}
		// We need 2 queries to pull details about the database (for now), sadly.
		else if(quote.equals("list")) {
			// First query to pull the total number of quotes
			PreparedStatement statement = database.getConnection().prepareStatement("SELECT COUNT(*) FROM Quotes");
			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next()) {
				int count = resultSet.getInt("COUNT(*)");
				// and the second to pull the most active shouter.
				statement = database.getConnection().prepareStatement("SELECT COUNT(Nick), Nick FROM Quotes GROUP BY Nick ORDER BY COUNT(Nick) DESC LIMIT 1");
				resultSet = statement.executeQuery();
				if(resultSet.next()) {
					// If both of these execute successfully (which they always should) then return the details the user asked for
					return "I have " + count + " quotes in my database. The most active shouter is " + resultSet.getString("Nick") + " with " + resultSet.getInt("COUNT(Nick)") + ".";
				}
			}
		}
		// You should know why by now.
		PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM Quotes WHERE Quote = ? AND Channel = ?");
		statement.setString(1, quote);
		statement.setString(2, event.getChannel().getName());
		ResultSet resultSet = statement.executeQuery();
		if(resultSet.next()) {
			// Tease the user if it's their own quote
			if(resultSet.getString("Nick").equals(event.getUser().getNick())) return "don't you remember? YOU submitted this! Put down the bong!";
			return resultSet.getString("Nick") + " shouted this about " + IRCUtils.toReadableTime((Date)resultSet.getTimestamp("Date"), false) + " ago.";
		} else {
			return "Quote not found.";
		}
	}

	// Method to add a new quote to the database.
	private void addNewQuote() throws SQLException {
		java.util.Date dt = new java.util.Date();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// Build and run our update against the database
		PreparedStatement statement = database.getConnection().prepareStatement("INSERT INTO Quotes(Nick, Date, Channel, Quote) VALUES (?, ?, ?, ?)");
		statement.setString(1, event.getUser().getNick());
		statement.setString(2, sdf.format(dt));
		statement.setString(3, event.getChannel().getName());
		statement.setString(4, event.getMessage());
		statement.executeUpdate();
	}
}
