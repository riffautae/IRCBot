package us.rddt.IRCBot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Configuration {
	private static String nick;
	private static String user;
	private static String server;
	private static int port;
	private static String password;
	private static String[] channels;
	
	private static String[] watchSubreddits;
	
	private static String admin_nick;
	private static String admin_hostmask;
	
	private static String database_driver;
	
	private static String mysql_server;
	private static String mysql_user;
	private static String mysql_password;
	private static String mysql_database;
	
	private static String sqlite_database;
	
	public static void loadConfiguration() throws FileNotFoundException, IOException {
		Properties config = new Properties();
		config.load(new FileInputStream("IRCBot.properties"));
		nick = config.getProperty("nick");
		user = config.getProperty("user");
		server = config.getProperty("server");
		port = Integer.parseInt(config.getProperty("port"));
		password = config.getProperty("password");
		channels = config.getProperty("channels").split(",");
		watchSubreddits = config.getProperty("watch_subreddits").split(",");
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
	}

	/**
	 * @return the nick
	 */
	public static String getNick() {
		return nick;
	}

	/**
	 * @return the user
	 */
	public static String getUser() {
		return user;
	}

	/**
	 * @return the server
	 */
	public static String getServer() {
		return server;
	}

	/**
	 * @return the port
	 */
	public static int getPort() {
		return port;
	}

	/**
	 * @return the password
	 */
	public static String getPassword() {
		return password;
	}

	/**
	 * @return the channels
	 */
	public static String[] getChannels() {
		return channels;
	}

	/**
	 * @return the watchSubreddits
	 */
	public static String[] getWatchSubreddits() {
		return watchSubreddits;
	}

	/**
	 * @return the admin_nick
	 */
	public static String getAdminNick() {
		return admin_nick;
	}

	/**
	 * @return the admin_host
	 */
	public static String getAdminHostmask() {
		return admin_hostmask;
	}

	/**
	 * @return the database_driver
	 */
	public static String getDatabaseDriver() {
		return database_driver;
	}

	/**
	 * @return the mysql_server
	 */
	public static String getMySQLServer() {
		return mysql_server;
	}

	/**
	 * @return the mysql_user
	 */
	public static String getMySQLUser() {
		return mysql_user;
	}

	/**
	 * @return the mysql_password
	 */
	public static String getMySQLPassword() {
		return mysql_password;
	}

	/**
	 * @return the mysql_database
	 */
	public static String getMySQLDatabase() {
		return mysql_database;
	}

	/**
	 * @return the sqlite_database
	 */
	public static String getSQLiteDatabase() {
		return sqlite_database;
	}
}
