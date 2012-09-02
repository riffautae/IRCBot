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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;

import org.pircbotx.PircBotX;

import us.rddt.IRCBot.Implementations.RedditWatcher;
import us.rddt.IRCBot.Logging.IRCLogger;

/**
 * Class which maintains the bot's configuration settings. Settings are loaded upon
 * bot startup and can be reloaded on the fly via a command provided by a bot
 * administrator.
 * 
 * @author Ryan Morrison
 */
public class Configuration {
    /*
     * Class variables.
     */
    private static String nick;
    private static String user;
    private static String server;
    private static int port;
    private static String password;
    private static boolean use_ssl;
    private static boolean ssl_verify;
    private static String[] channels;
    
    private static String channel_announcement;
    private static String[] channel_participating;
    
    private static List<String> disabled_functions;

    private static String[] watchSubreddits;
    
    private static int votekickDuration;
    private static int votekickPassPercent;

    private static String admin_nick;
    private static String admin_hostmask;

    private static String database_driver;

    private static String mysql_server;
    private static String mysql_user;
    private static String mysql_password;
    private static String mysql_database;

    private static String sqlite_database;

    private static ScheduledExecutorService scheduler;
    
    private static IRCLogger logger;
    private static String log_output;
    
    private static String user_agent;

    /**
     * Loads the configuration provided via a properties file
     * @throws FileNotFoundException if the properties file does not exist
     * @throws IOException if an exception is raised reading the properties file
     */
    public static void loadConfiguration() throws FileNotFoundException, IOException {
        Properties config = new Properties();
        config.load(new FileInputStream("IRCBot.properties"));
        nick = config.getProperty("nick");
        user = config.getProperty("user");
        server = config.getProperty("server");
        port = Integer.parseInt(config.getProperty("port"));
        password = config.getProperty("password");
        use_ssl = Boolean.parseBoolean(config.getProperty("use_ssl"));
        ssl_verify = Boolean.parseBoolean(config.getProperty("ssl_verify"));
        channels = config.getProperty("channels").split(",");
        channel_announcement = config.getProperty("channel_announcement");
        channel_participating = config.getProperty("channel_participating").split(",");
        disabled_functions = new ArrayList<String>(Arrays.asList(config.getProperty("disabled_functions").split(",")));
        watchSubreddits = config.getProperty("watch_subreddits").split(",");
        votekickDuration = Integer.parseInt(config.getProperty("votekick_duration"));
        votekickPassPercent = Integer.parseInt(config.getProperty("votekick_pass_percent"));
        admin_nick = config.getProperty("admin_nick");
        admin_hostmask = config.getProperty("admin_hostmask");
        database_driver = config.getProperty("database_driver");
        if(database_driver.equalsIgnoreCase("mysql")) {
            mysql_server = config.getProperty("mysql_server");
            mysql_user = config.getProperty("mysql_user");
            mysql_password = config.getProperty("mysql_password");
            mysql_database = config.getProperty("mysql_database");
        } else if(database_driver.equalsIgnoreCase("sqlite")) {
            sqlite_database = config.getProperty("sqlite_database");
        }
        log_output = config.getProperty("log_output");
        user_agent = config.getProperty("user_agent");
    }

    /**
     * Starts the scheduler(s) (if needed) to monitor configured subreddits
     * @param bot the IRC bot
     */
    public static void startScheduler(PircBotX bot) {
        if(watchSubreddits.length > 0 && !watchSubreddits[0].equals("") && !disabled_functions.contains("watcher")) {
            if(scheduler != null) {
                Configuration.getLogger().write(Level.INFO, "Shutting down existing subreddit updates");
                scheduler.shutdownNow();
            }
            scheduler = Executors.newScheduledThreadPool(watchSubreddits.length);
            for(int i = 0; i < watchSubreddits.length; i++) {
                String[] configuration = watchSubreddits[i].split(":");
                String subreddit = configuration[0];
                int frequency = Integer.parseInt(configuration[1]);
                Configuration.getLogger().write(Level.INFO, "Scheduling subreddit updates for r/" + subreddit + " starting in " + (5 * i) + " minutes (frequency: " + frequency + " minutes)");
                scheduler.scheduleWithFixedDelay(new RedditWatcher(bot, subreddit), (5 * i), frequency, TimeUnit.MINUTES);
            }
        }
    }

    /**
     * Returns the application's version string from the manifest.
     * @return the application version string
     */
    public static String getApplicationVersion() {
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
            while(resources.hasMoreElements()) {
                Manifest manifest = new Manifest(((URL)resources.nextElement()).openStream());
                Attributes mainAttribs = (Attributes)manifest.getMainAttributes();
                if(mainAttribs.getValue("Build-Version") != null) return mainAttribs.getValue("Build-Version");
            }
            return null;
        } catch (IOException ex) {
            getLogger().write(Level.WARNING, IRCUtils.getStackTraceString(ex));
            return null;
        }
    }

    /**
     * Returns the bot's nickname
     * @return the bot's nickname
     */
    public static String getNick() {
        return nick;
    }

    /**
     * Returns the bot's username
     * @return the bot's username
     */
    public static String getUser() {
        return user;
    }

    /**
     * Returns the server to connect to
     * @return the server to connect to
     */
    public static String getServer() {
        return server;
    }

    /**
     * Returns the server's port
     * @return the server's port
     */
    public static int getPort() {
        return port;
    }

    /**
     * Returns the server's password
     * @return the server's password
     */
    public static String getPassword() {
        return password;
    }
    
    /**
     * Returns the channels to join
     * @return the channels to join
     */
    public static String[] getChannels() {
        return channels;
    }
    
    /**
     * Returns the channel announcement
     * @return the channel announcement
     */
    public static String getChannelAnnouncement() {
        return channel_announcement;
    }
    
    /**
     * Returns the list of participating channels to send announcements in
     * @return the list of participating channels to send announcements in
     */
    public static String[] getChannelsParticipating() {
        return channel_participating;
    }
    
    /**
     * Returns the list of functions to disable
     * @return the list of functions to disable
     */
    public static List<String> getDisabledFunctions() {
        return disabled_functions;
    }
    
    /**
     * Returns the subreddits to watch
     * @return the subreddits to watch
     */
    public static String[] getWatchSubreddits() {
        return watchSubreddits;
    }
    
    /**
     * Returns the duration of votekicks in seconds
     * @return the duration of votekicks in seconds
     */
    public static int getVotekickDuration() {
        return votekickDuration;
    }
    
    /**
     * Returns the percentage required for a votekick to pass
     * @return the percentage required for a votekick to pass
     */
    public static int getVotekickPassPercent() {
        return votekickPassPercent;
    }
    /**
     * Returns the nick of the administrator
     * @return the nick of the administrator
     */
    public static String getAdminNick() {
        return admin_nick;
    }

    /**
     * Returns the hostmask of the administrator
     * @return the hostmask of the administrator
     */
    public static String getAdminHostmask() {
        return admin_hostmask;
    }

    /**
     * Returns the database driver to use
     * @return the database driver to use
     */
    public static String getDatabaseDriver() {
        return database_driver;
    }

    /**
     * Returns the MySQL server
     * @return the MySQL server
     */
    public static String getMySQLServer() {
        return mysql_server;
    }

    /**
     * Returns the MySQL user
     * @return the MySQL user
     */
    public static String getMySQLUser() {
        return mysql_user;
    }

    /**
     * Returns the MySQL password
     * @return the MySQL password
     */
    public static String getMySQLPassword() {
        return mysql_password;
    }

    /**
     * Returns the MySQL database
     * @return the MySQL database
     */
    public static String getMySQLDatabase() {
        return mysql_database;
    }

    /**
     * Returns the SQLite database
     * @return the SQLite database
     */
    public static String getSQLiteDatabase() {
        return sqlite_database;
    }
    
    /**
     * Returns the logger for use if it exists, otherwise initialize and return a new one
     * @return the logger to use
     */
    public static IRCLogger getLogger() {
        if(logger == null) {
            logger = new IRCLogger();
            try {
                logger.setup();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return logger;
    }
    
    public static String getUserAgent() {
        if(user_agent != null && !user_agent.isEmpty()) {
            return user_agent;
        }
        return "Mozilla/5.0 (Windows NT 6.1; rv:6.0) Gecko/20110814 Firefox/6.0";
    }
    
    /**
     * Returns the log file to output the HTML-formatted log to
     * @return the log file to output the HTML-formatted log to
     */
    public static String getLogFile() {
        return log_output;
    }
    
    /**
     * Returns if the connection should be secured through SSL
     * @return true if SSL should be used, false for unsecured connections
     */
    public static boolean isSSL() {
        return use_ssl;
    }
    
    /**
     * Returns if the SSL connection should verify certificates
     * @return true for SSL certificate verification, false to trust all certificates
     */
    public static boolean isSSLVerified() {
        return ssl_verify;
    }
}
