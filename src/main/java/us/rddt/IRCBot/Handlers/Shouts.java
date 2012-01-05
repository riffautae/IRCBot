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

package us.rddt.IRCBot.Handlers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import us.rddt.IRCBot.Database;
import us.rddt.IRCBot.IRCUtils;
import us.rddt.IRCBot.Enums.LogLevels;

/**
 * @author Ryan Morrison
 */
class Shout {
	/*
	 * Class variables.
	 */
	private String quote;
	private String readableDate;
	private String submitter;
	
	/**
	 * Class constructor
	 * @param quote the quote to store
	 * @param submitter the user's nickname who submitted the quote
	 * @param readableDate the date of the quote's submission in human-readable format
	 */
	public Shout(String quote, String submitter, String readableDate) {
		this.quote = quote;
		this.submitter = submitter;
		this.readableDate = readableDate;
	}

	/**
	 * Returns the quote
	 * @return the quote
	 */
	public String getQuote() {
		return quote;
	}

	/**
	 * Returns the human-readable date of submission
	 * @return the human-readable date of submission
	 */
	public String getReadableDate() {
		return readableDate;
	}

	/**
	 * Returns the quote's submitter
	 * @return the quote's submitter
	 */
	public String getSubmitter() {
		return submitter;
	}
}

/**
 * @author Ryan Morrison
 */
public class Shouts implements Runnable {
	/*
	 * Class variables.
	 */
	private static volatile Map<String,Shout> shoutMap = Collections.synchronizedMap(new HashMap<String,Shout>());
	private Database database;
	private MessageEvent<PircBotX> event = null;
	private boolean isRandomShout = false;
	private int quoteNumber;
	private String randomQuote = null;

	/**
	 * Class constructor
	 * @param event the MessageEvent that triggered this class
	 */
	public Shouts(MessageEvent<PircBotX> event) {
		this.event = event;
	}

	/**
	 * Overloadable class constructor
	 * @param event the MessageEvent that triggered this class
	 * @param isRandomShout true if the user requested a random shout, false if the user is looking up a quote
	 */
	public Shouts(MessageEvent<PircBotX> event, boolean isRandomShout) {
		this.event = event;
		this.isRandomShout = isRandomShout;
	}

	/**
	 * Adds a new quote to the database
	 * @throws SQLException if the SQL query does not execute correctly
	 */
	private void addNewQuote() throws SQLException {
		// Build and run our update against the database
		PreparedStatement statement = database.getConnection().prepareStatement("INSERT INTO Quotes(Nick, Date, Channel, Quote) VALUES (?, ?, ?, ?)");
		statement.setString(1, event.getUser().getNick());
		statement.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
		statement.setString(3, event.getChannel().getName());
		statement.setString(4, event.getMessage());
		statement.executeUpdate();
	}

	/**
	 * Check to see if a quote exists
	 * @return true if the quote exists, false if it does not
	 * @throws SQLException if the SQL query does not execute correctly
	 */
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

	/**
	 * Returns the appropriate shout class for the current channel
	 * @return the appropriate shout class
	 */
	private Shout getLastShout() {
		return shoutMap.get(event.getChannel().getName());
	}

	/**
	 * Return information about a quote provided by the user
	 * @param quote the quote to look up in the database
	 * @return the formatted result of the database lookup
	 * @throws SQLException if the SQL query does not execute correctly
	 */
	private String getQuoteInfo(String quote) throws SQLException {
		// Variable to track whether the user requested the last quote from the bot
		// If the user wants the last quote, prevent hitting the database twice and simply retrieve details from memory
		if(quote.equals("last")) {
			// On startup there is no previous quote, so return as such if a user attempts a !who last
			if(getLastShout() == null) return "No previous quote.";
			// Tease the user if it's their own quote
			if(getLastShout().getSubmitter().equals(event.getUser().getNick())) return Colors.BOLD + "YOU" + Colors.NORMAL + " taught me that! (Don't you remember? Put down the bong!) about " + getLastShout().getReadableDate() + " ago.";
			// Provide context if the !who last command was used, but trim it if the quote is longer than 10 characters (use 60% of the quote instead)
			if(quote.length() < 11) {
				return getLastShout().getSubmitter() + " shouted \"" + getLastShout().getQuote() + "\" about " + getLastShout().getReadableDate() + " ago.";
			} else {
				return getLastShout().getSubmitter() + " shouted \"" + getLastShout().getQuote().substring(0, (int)(getLastShout().getQuote().length() * 0.6)) + "...\" " + getLastShout().getReadableDate() + ".";
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
			if(resultSet.getString("Nick").equals(event.getUser().getNick())) return Colors.BOLD + "YOU" + Colors.NORMAL + " taught me that! (Don't you remember? Put down the bong!) about " + IRCUtils.toReadableTime((Date)resultSet.getTimestamp("Date"), false) + " ago.";
			return resultSet.getString("Nick") + " shouted this about " + IRCUtils.toReadableTime((Date)resultSet.getTimestamp("Date"), false) + " ago.";
		} else {
			return "Quote not found.";
		}
	}
	
	/**
	 * Return the quote at a provided line number
	 * @param line the line number to return
	 * @return the quote at the provided line
	 * @throws SQLException if the SQL query does not execute correctly
	 */
	private String getQuoteLine(int line) throws SQLException {
		if(line < 1) return "Line number " + line + " is invalid.";
		PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM Quotes WHERE Channel = ? LIMIT ?,1");
		statement.setString(1, event.getChannel().getName());
		statement.setInt(2, line - 1);
		ResultSet resultSet = statement.executeQuery();
		if(resultSet.next()) {
			return "Quote #" + line + " (" + resultSet.getString("Quote") + ") was shouted by " + resultSet.getString("Nick") + " about " + IRCUtils.toReadableTime((Date)resultSet.getTimestamp("Date"), false) + " ago.";
		} else {
			return "Quote #" + line + " not found.";
		}
	}
	
	/**
	 * Returns a randomly selected quote from the database
	 * @return the randomly selected quote
	 * @throws SQLException if the SQL query does not execute correctly
	 */
	private String getRandomQuote() throws SQLException {
		// We use prepared statements to sanitize input from the user
		// Specifying the channel allows different channels to have their own list of quotes available
		PreparedStatement statement= database.getConnection().prepareStatement("SELECT * FROM Quotes WHERE Channel = ? ORDER BY RAND() LIMIT 1");
		statement.setString(1, event.getChannel().getName());
		// Execute our query against the database
		ResultSet resultSet = statement.executeQuery();
		if(resultSet.next()) {
			// Save the last quote to prevent an extra DB hit on !who last
			shoutMap.put(event.getChannel().getName(), new Shout(resultSet.getString("Quote"), resultSet.getString("Nick"), IRCUtils.toReadableTime((Date)resultSet.getTimestamp("Date"), false)));
			// Return the random quote
			return resultSet.getString("Quote");
		} else {
			// The database query returned nothing, so return null
			return null;
		}
	}

	/**
	 * Return if the provided quote is a valid quote number
	 * @param command the quote to parse
	 * @return true if the quote is a valid quote number, false if it is not valid
	 */
	private boolean isValidQuoteNumber(String command) {
		try {
			quoteNumber = Integer.parseInt(command);
			if(quoteNumber >= 0) {
				return true;
			}
			return false;
		} catch (NumberFormatException ex) {
			return false;
		}
	}
	
	/**
	 * Method that executes upon thread start
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
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
					event.getBot().sendMessage(event.getChannel(), (Colors.removeFormattingAndColors(randomQuote)));
				}
				if(!doesQuoteExist()) addNewQuote();
			} else {
				// We're dealing with a !who command - respond to the user with the information about the quote.
				String whoCommand = event.getMessage().substring(5).replaceAll("^\\s+", "").replaceAll("\\s+$", "");
				if(isValidQuoteNumber(whoCommand)) {
					event.respond(getQuoteLine(quoteNumber));
					return;
				}
				try {
					event.respond(getQuoteInfo(whoCommand));
				} catch (IndexOutOfBoundsException ex) {
					return;
				}
			}
			// Disconnect from the database
			database.disconnect();
		} catch (Exception ex) {
			IRCUtils.Log(LogLevels.ERROR, ex.getMessage());
			ex.printStackTrace();
			return;
		}
	}
}
