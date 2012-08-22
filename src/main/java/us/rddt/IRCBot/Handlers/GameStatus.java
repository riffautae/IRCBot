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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import us.rddt.IRCBot.Configuration;
import us.rddt.IRCBot.Database;
import us.rddt.IRCBot.IRCUtils;

public class GameStatus implements Runnable {
    // Variables
    private MessageEvent<PircBotX> event;
    private Database database;
    private HashMap<String, String> gamesMap = new HashMap<String, String>();

    /**
     * Class constructor
     * @param event the MessageEvent that triggered this class
     */
    public GameStatus(MessageEvent<PircBotX> event) {
        this.event = event;
    }

    private void getGameStatus(String game) throws ClassNotFoundException, SQLException, IOException {
        // Check to see if the game exists in the HashMap and if so update accordingly
        if(gamesMap.containsKey(game)) {
            // Get the game's full title
            game = gamesMap.get(game);
            // Boolean value to determine if results were returned or not
            boolean emptyRows = true;

            // Connect to the database
            database = new Database();
            database.connect();
            // Prepare the StringBuilder to hold the list of nicks playing
            StringBuilder builder = new StringBuilder();

            // Prepare and execute the SQL query
            PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM GameStatus WHERE Game = ?");
            statement.setString(1, game);
            ResultSet resultSet = statement.executeQuery();

            builder.append("Users playing " + game + ": ");

            // If a result was returned, tell the channel what the user is playing
            // Otherwise, they aren't playing anything
            String prefix = "";
            while(resultSet.next()) {
                builder.append(prefix);
                prefix = ", ";
                builder.append(resultSet.getString("Nick") + " (" + IRCUtils.toReadableTime(resultSet.getTimestamp("Date"), false, false) + ")");
                emptyRows = false;
            }

            // Disconnect from the database
            database.disconnect();

            /*
             * JDBC does not provide a clear method of determining whether a ResultSet actually has any rows.
             * We have to use a boolean to work out whether it actually returned anything.
             */
            if(emptyRows) builder.append("nobody");

            // Return the result
            event.getBot().sendMessage(event.getChannel(), builder.toString());
        } else {
            // The game's full title isn't in the HashMap
            throw new IllegalArgumentException("Game does not exist");
        }
    }

    /**
     * Returns the status of a given user to the channel
     * @param nick the nick to retrieve the status of
     * @throws ClassNotFoundException if the database class cannot be found
     * @throws SQLException if the SQL query fails
     * @throws IOException if reading from the ResultSet fails
     */
    private void getUserStatus(String nick) throws ClassNotFoundException, SQLException, IOException {
        // Connect to the database
        database = new Database();
        database.connect();

        // Prepare and execute the SQL query
        PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM GameStatus WHERE Nick = ?");
        statement.setString(1, nick);
        ResultSet resultSet = statement.executeQuery();

        // If a result was returned, tell the channel what the user is playing
        // Otherwise, they aren't playing anything
        if(resultSet.next()) {
            event.getBot().sendMessage(event.getChannel(), nick + " is playing " + resultSet.getString("Game") + " (" + IRCUtils.toReadableTime(resultSet.getTimestamp("Date"), false, false) + ")");
        } else {
            event.getBot().sendMessage(event.getChannel(), nick + " is not playing anything!");
        }

        // Disconnect from the database
        database.disconnect();
    }

    /**
     * Loads full game titles from a file into a HashMap for easy access
     * @throws FileNotFoundException if the file containing the titles cannot be found
     */
    private void loadGameTitles() throws FileNotFoundException {
        // Holds the string of the current line being read
        String currentLine = "";

        // Open the file for reading
        BufferedReader reader = new BufferedReader(new FileReader("games.txt"));
        try {
            // Loop through each line
            while((currentLine = reader.readLine()) != null) {
                // Split the line with a comma delimiter, and add it to the HashMap
                String[] lineSplit = currentLine.split(",");
                gamesMap.put(lineSplit[0], lineSplit[1]);
            }
        } catch(IOException ex) {
            Configuration.getLogger().write(Level.WARNING, ex.getStackTrace().toString());
        } finally {
            try {
                // Close the file
                if(reader != null) reader.close();
            }
            catch (IOException ex) {
                Configuration.getLogger().write(Level.WARNING, ex.getStackTrace().toString());
            }
        }
    }

    /**
     * Resets the given user's status (deletes the database entry)
     * @param nick the nick to retrieve the status of
     * @throws ClassNotFoundException if the database class cannot be found
     * @throws SQLException if the SQL query fails
     * @throws IOException if reading from the ResultSet fails
     */
    private void resetUserStatus(String nick) throws ClassNotFoundException, SQLException, IOException {
        // Connect to the database
        database = new Database();
        database.connect();

        // Prepare and execute the query to delete any entry
        PreparedStatement statement = database.getConnection().prepareStatement("DELETE FROM GameStatus WHERE Nick = ?");
        statement.setString(1, nick);
        statement.executeUpdate();

        // Disconnect from the database
        database.disconnect();
    }

    /**
     * Sets or updates the given user's status
     * @param nick the nick to update the status for
     * @param game the shortened game string
     * @throws ClassNotFoundException if the database class cannot be found
     * @throws SQLException if the SQL query fails
     * @throws IOException if reading from the ResultSet fails
     * @throws IllegalArgumentException if the game's full string doesn't exist in the HashMap
     */
    private void setUserStatus(String nick, String game) throws ClassNotFoundException, SQLException, IOException, IllegalArgumentException {
        // Prepare the database object
        database = new Database();

        // Check to see if the game exists in the HashMap and if so update accordingly
        if(gamesMap.containsKey(game)) {
            // Get the game's full title
            game = gamesMap.get(game);

            // Connect to the database
            database.connect();
            // Prepare the query to check if an entry already exists and execute it
            PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM GameStatus WHERE Nick = ?");
            statement.setString(1, nick);
            ResultSet resultSet = statement.executeQuery();
            // If there is already a game, update it instead of creating a brand new entry
            if(resultSet.next()) {
                // Close the previous statement if it isn't closed already
                if(!statement.isClosed()) statement.close();
                // Prepare and execute the SQL query to update
                statement = database.getConnection().prepareStatement("UPDATE GameStatus SET Game = ?, Date = ? WHERE Nick = ?");
                statement.setString(1, game);
                statement.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                statement.setString(3, nick);
                statement.executeUpdate();
            } else {
                // Close the previous statement if it isn't closed already
                if(!statement.isClosed()) statement.close();
                // Prepare and execute the SQL query to insert
                statement = database.getConnection().prepareStatement("INSERT INTO GameStatus(Nick, Date, Game) VALUES (?, ?, ?)");
                statement.setString(1, nick);
                statement.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                statement.setString(3, game);
                statement.executeUpdate();
            }

            // Disconnect from the database
            database.disconnect();
        } else {
            // The game's full title isn't in the HashMap
            throw new IllegalArgumentException("Game does not exist");
        }
    }

    /**
     * Method that executes upon thread start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        // Load the list of game titles into the HashMap
        // If they cannot be loaded, just return
        try {
            this.loadGameTitles();
        } catch (FileNotFoundException ex) {
            Configuration.getLogger().write(Level.WARNING, ex.getStackTrace().toString());
            return;
        }
        // Split the message into parameters
        String[] parameters = event.getMessage().split(" ");
        // Check the parameters and perform the appropriate actions
        if(parameters[1].equalsIgnoreCase("set")) {
            if(parameters.length > 2) {
                try {
                    setUserStatus(event.getUser().getNick(), parameters[2]);
                } catch(Exception ex) {
                    event.respond("Unable to set status - " + ex.getMessage());
                    return;
                }
                event.respond("Done!");
            } else {
                event.respond("You must provide a game to play!");
            }
        } else if(parameters[1].equalsIgnoreCase("reset")) {
            try {
                resetUserStatus(event.getUser().getNick());
            } catch(Exception ex) {
                event.respond("Unable to reset status - " + ex.getMessage());
                return;
            }
            event.respond("Done!");
        } else if(parameters[1].equalsIgnoreCase("game")) {
            if(parameters.length > 2) {
                try {
                    getGameStatus(parameters[2]);
                } catch(Exception ex) {
                    event.respond("Unable to get status - " + ex.getMessage());
                    return;
                }
            } else {
                event.respond("You must provide a game!");
            }
        } else {
            try {
                getUserStatus(parameters[1]);
            } catch (Exception ex) {
                event.respond("Unable to get status - " + ex.getMessage());
            }
        }
    }
}
