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
import java.util.logging.Level;

import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import us.rddt.IRCBot.Configuration;
import us.rddt.IRCBot.Database;
import us.rddt.IRCBot.IRCUtils;

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
    private ShoutEvents eventType;
    private int quoteNumber;
    private String randomQuote = null;
    
    public enum ShoutEvents {
        RANDOM_SHOUT,
        LOOKUP_COMMAND,
        LAST_COMMAND,
        LIST_COMMAND,
        TOP10_COMMAND,
        DELETE_COMMAND,
    }

    /**
     * Class constructor
     * @param event the MessageEvent that triggered this class
     */
    public Shouts(MessageEvent<PircBotX> event, ShoutEvents eventType) {
        this.event = event;
        this.eventType = eventType;
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
     * Deletes a quote from the database
     * @param quote the quote text to delete
     * @throws SQLException if the SQL query does not execute correctly
     */
    private int deleteQuote(String quote) throws SQLException {
        PreparedStatement statement = database.getConnection().prepareStatement("DELETE FROM Quotes WHERE Quote = ? AND Channel = ?");
        statement.setString(1, quote);
        statement.setString(2, event.getChannel().getName());
        return statement.executeUpdate();
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
    private Shout getShoutMap() {
        return shoutMap.get(event.getChannel().getName());
    }
    
    /**
     * Returns the last shout if possible
     * @return the last shout (if possible)
     */
    private String getLastShout() {
        // On startup there is no previous quote, so return as such if a user attempts a !who last
        if(getShoutMap() == null) return "No previous quote.";
        // Tease the user if it's their own quote
        if(getShoutMap().getSubmitter().equals(event.getUser().getNick())) return Colors.BOLD + "YOU" + Colors.NORMAL + " taught me that! (Don't you remember? Put down the bong!) about " + getShoutMap().getReadableDate() + " ago.";
        // Provide context if the !who last command was used, but trim it if the quote is longer than 10 characters (use 60% of the quote instead)
        if(getShoutMap().getQuote().length() < 11) {
            return getShoutMap().getSubmitter() + " shouted \"" + getShoutMap().getQuote() + "\" about " + getShoutMap().getReadableDate() + " ago.";
        } else {
            return getShoutMap().getSubmitter() + " shouted \"" + getShoutMap().getQuote().substring(0, (int)(getShoutMap().getQuote().length() * 0.6)) + "...\" about " + getShoutMap().getReadableDate() + " ago.";
        }
    }

    /**
     * Return information about a quote provided by the user
     * @param quote the quote to look up in the database
     * @return the formatted result of the database lookup
     * @throws SQLException if the SQL query does not execute correctly
     */
    private String getQuoteInfo(String quote) throws SQLException {
        // You should know why by now.
        PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM Quotes WHERE Quote = ? AND Channel = ?");
        statement.setString(1, quote);
        statement.setString(2, event.getChannel().getName());
        ResultSet resultSet = statement.executeQuery();
        if(resultSet.next()) {
            // Tease the user if it's their own quote
            if(resultSet.getString("Nick").equals(event.getUser().getNick())) return Colors.BOLD + "YOU" + Colors.NORMAL + " taught me that! (Don't you remember? Put down the bong!) about " + IRCUtils.toReadableTime((Date)resultSet.getTimestamp("Date"), false, true) + " ago.";
            return resultSet.getString("Nick") + " shouted this about " + IRCUtils.toReadableTime((Date)resultSet.getTimestamp("Date"), false, true) + " ago.";
        } else {
            return "Quote not found.";
        }
    }
    
    /**
     * Returns the statistics of the quote database
     * @return the formatted statistics of the quote database
     * @throws SQLException if the SQL query does not execute correctly
     */
    private String getQuoteStats() throws SQLException {
        // First query to pull the total number of quotes
        PreparedStatement statement = database.getConnection().prepareStatement("SELECT COUNT(*) FROM Quotes WHERE Channel = ?");
        statement.setString(1, event.getChannel().getName());
        ResultSet resultSet = statement.executeQuery();
        if(resultSet.next()) {
            int count = resultSet.getInt("COUNT(*)");
            // and the second to pull the most active shouter.
            statement = database.getConnection().prepareStatement("SELECT COUNT(Nick), Nick FROM Quotes WHERE Channel = ? GROUP BY Nick ORDER BY COUNT(Nick) DESC LIMIT 1");
            statement.setString(1, event.getChannel().getName());
            resultSet = statement.executeQuery();
            if(resultSet.next()) {
                // If both of these execute successfully (which they always should) then return the details the user asked for
                return "I have " + count + " quotes in my database. The most active shouter is " + resultSet.getString("Nick") + " with " + resultSet.getInt("COUNT(Nick)") + ".";
            }
        }
        return null;
    }

    /**
     * Return the quote at a provided line number
     * @param line the line number to return
     * @return the quote at the provided line
     * @throws SQLException if the SQL query does not execute correctly
     */
    private String getQuoteLine(int line) throws SQLException {
        PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM Quotes WHERE Channel = ? LIMIT ?,1");
        statement.setString(1, event.getChannel().getName());
        statement.setInt(2, line - 1);
        ResultSet resultSet = statement.executeQuery();
        if(resultSet.next()) {
            return "Quote #" + line + " (" + resultSet.getString("Quote") + ") was shouted by " + resultSet.getString("Nick") + " about " + IRCUtils.toReadableTime((Date)resultSet.getTimestamp("Date"), false, true) + " ago.";
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
            shoutMap.put(event.getChannel().getName(), new Shout(resultSet.getString("Quote"), resultSet.getString("Nick"), IRCUtils.toReadableTime((Date)resultSet.getTimestamp("Date"), false, true)));
            // Return the random quote
            return resultSet.getString("Quote");
        } else {
            // The database query returned nothing, so return null
            return null;
        }
    }
    
    /**
     * Returns the top 10 shouters on the channel
     * @return the formatted top 10 shouters on the channel
     * @throws SQLException if the SQL query does not execute correctly
     */
    private String getTop10Shouters() throws SQLException {
        int tempCount = 1;
        // A temporary StringBuilder to construct our top 10 list
        StringBuilder constructedString = new StringBuilder();
        constructedString.append("The top 10 shouters in " + event.getChannel().getName() + ": ");
        // We use prepared statements to sanitize input from the user
        // Specifying the channel allows different channels to have their own list of quotes available
        PreparedStatement statement = database.getConnection().prepareStatement("SELECT COUNT(Nick), Nick FROM Quotes WHERE Channel = ? GROUP BY Nick ORDER BY COUNT(Nick) DESC LIMIT 10");
        statement.setString(1, event.getChannel().getName());
        // Execute our query against the database
        ResultSet resultSet = statement.executeQuery();
        while(resultSet.next()) {
            constructedString.append(tempCount + ": " + resultSet.getString("Nick") + " (" + resultSet.getInt("COUNT(Nick)") + "), ");
            tempCount++;
        }
        return constructedString.toString().substring(0, constructedString.length() - 2);
    }

    /**
     * Return if the provided quote is a valid quote number
     * @param command the quote to parse
     * @return true if the quote is a valid quote number, false if it is not valid
     */
    private boolean isValidQuoteNumber(String command) {
        try {
            quoteNumber = Integer.parseInt(command);
            if(quoteNumber > 0) {
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
            // If we're to return a random shout
            if(eventType.equals(ShoutEvents.RANDOM_SHOUT)) {
                // Get a random quote from the database (if possible). Send it to the channel.
                // If the quote does not exist in the database, add it!
                if((randomQuote = getRandomQuote()) != null) {
                    event.getBot().sendMessage(event.getChannel(), (Colors.removeFormattingAndColors(randomQuote)));
                }
                if(!doesQuoteExist()) addNewQuote();
            } else if(eventType.equals(ShoutEvents.LOOKUP_COMMAND)) {
                // We're dealing with a !who list command - respond to the user with the information about the quote.
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
            } else if(eventType.equals(ShoutEvents.LIST_COMMAND)) {
                // We're dealing with a !who list command - respond to the user with the quote database's statistics
                event.respond(getQuoteStats());
            } else if(eventType.equals(ShoutEvents.LAST_COMMAND)) {
                // We're dealing with a !who last command - respond to the user with the last shout
                event.respond(getLastShout());
            } else if(eventType.equals(ShoutEvents.TOP10_COMMAND)) {
                // We're dealing with a !who top10 command - respond to the user with the top 10 users
                event.respond(getTop10Shouters());
            } else if(eventType.equals(ShoutEvents.DELETE_COMMAND)) {
                // We're dealing with a !who delete command - delete the provided quote from the database
                // Operator status has already been confirmed at this point
                if(deleteQuote(event.getMessage().split("!who delete ")[1]) > 0) {
                    event.respond("Quote has been removed from the database.");
                } else {
                    event.respond("Could not delete quote - quote not found.");
                }
            }
            // Disconnect from the database
            database.disconnect();
        } catch (Exception ex) {
            Configuration.getLogger().write(Level.WARNING, ex.getStackTrace().toString());
            return;
        }
    }
}
