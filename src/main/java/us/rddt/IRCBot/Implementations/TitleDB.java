package us.rddt.IRCBot.Implementations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;

import org.pircbotx.Channel;
import org.pircbotx.User;

import us.rddt.IRCBot.Struct.TwoItemStruct;

/**
 * Helper class to do the db work
 * @author Milton Thomas
 */
public class TitleDB {
	
	/* announce:
	 * on join or too much monologue 
	 * 	get random title
	 */
	
	/*
	 * control:
 	 * on pm
	 *  for user
	 *   add title
	 *   list title[s]
	 *   rem title
	 */
	
	// singleton time
	private static TitleDB _title = new TitleDB();
	
	public static TitleDB instanceOf() {
		return _title;
	}
	
	/**
	 * Get a random title
	 * @param conn Db connection
	 * @param chan channel this took place in
	 * @param user for who
	 * @return The id, the title
	 * @throws SQLException
	 */
	public synchronized TwoItemStruct<Integer, String> title(Connection conn, Channel chan, User user) throws SQLException {
		String title = null;
		Integer id = null;
		PreparedStatement sCount = conn.prepareStatement(
				"SELECT COUNT(Nick) FROM Titles WHERE Channel = ? AND Nick = ?");
		sCount.setString(1, chan.getName());
		sCount.setString(2, user.getNick());

		ResultSet result = sCount.executeQuery();
		if (result.next()) {
        	int count = result.getInt(1);
        	if(count == 0) return null;
        	int item = (new Random()).nextInt(count);
        	
			PreparedStatement statement = conn.prepareStatement(
					"SELECT Id, Intro FROM Titles WHERE Nick = ? LIMIT ?, 1");
	        statement.setString(1, user.getNick());
	        statement.setInt(2, item);
	        
	        result = statement.executeQuery();
	        if (result.next()) {
	        	id = result.getInt(1);
	        	title = result.getString(2);
	        	return new TwoItemStruct<Integer, String>(id, title);
	        }
		}
        return null;
	}

	/**
	 * Add an title to the db
	 * @param conn db connection
	 * @param submitter User who is adding
	 * @param chan channel to add it to
	 * @param victim user who gets this new title
	 * @param title the string for the title
	 * @throws SQLException
	 */
	public synchronized int addTitle(Connection conn, String submitter, String chan, String victim, String title) throws SQLException {
		PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO Titles(Submitter, Date, Nick, Channel, Intro) Values (?,?,?,?,?)");
		statement.setString(1, submitter);
		statement.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
		statement.setString(3, victim);
		statement.setString(4, chan);
		statement.setString(5, title);
		
		return statement.executeUpdate();
	}
	
	public synchronized int remTitleByString(Connection conn, String chan, String nick, String title) throws SQLException {
		PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM Titles WHERE Channel = ? AND Nick = ? AND Intro = ?");
		
		statement.setString(1, chan);
		statement.setString(2, nick);
		statement.setString(3, title);
		
		return statement.executeUpdate();
	}
	
	public synchronized int remTitleById(Connection conn, String channel, String user, int id) throws SQLException {
		PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM Titles WHERE Channel = ? AND User = ? AND id = ?");
		statement.setString(1, channel);
		statement.setString(2, user);
		statement.setInt(3, id);
		
		return statement.executeUpdate();
	}
	
	/**
	 * Returns a list of titles and the id
	 * @param conn db connection
	 * @param channel the irc channel
	 * @param of whos titles
	 * @param page which page of titles
	 * @return List of [id, title]
	 * @throws SQLException
	 */
	public synchronized List<String[]> listTitles(Connection conn, String channel, String of, int page) throws SQLException {
		final int PAGE_LENGTH = 4;
		PreparedStatement statement = conn.prepareStatement(
				"SELECT Id, Title FROM Titles WHERE Channel = ? AND User = ? LIMIT ?, ?");
		statement.setString(1, channel);
		statement.setString(2, of);
		statement.setInt(3, page * PAGE_LENGTH + 1);
		statement.setInt(4, PAGE_LENGTH);
		
		ResultSet rs = statement.executeQuery();
		
		LinkedList<String[]> resp = new LinkedList<String[]>();
		String[] line;
		while (rs.next()) {
			line = new String[2];
			line[0] = rs.getString(1); // id
			line[1] = rs.getString(2); // title
			resp.add(line);
		}
		return resp;
	}
	
	/**
	 * Get some top user channel statistics
	 * @param conn db connection
	 * @param channel irc channel
	 * @return Array of [title count, jerk name, jerk count, victim name, victim count]
	 * @throws SQLException
	 */
	public synchronized String[] getStats(Connection conn, String channel) throws SQLException {
		String[] ret = new String[5];
		// First query to pull the total number of titles
        PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM Titles WHERE Channel = ?");
        statement.setString(1, channel);
        ResultSet resultSet = statement.executeQuery();
        if(!resultSet.next()) return null;
        ret[0] = String.valueOf( resultSet.getInt("COUNT(*)") );
        
        // the biggest jerk
        statement = conn.prepareStatement("SELECT COUNT(Nick), Nick FROM Titles WHERE Channel = ? GROUP BY Nick ORDER BY COUNT(Nick) DESC LIMIT 1");
        statement.setString(1, channel);
        resultSet = statement.executeQuery();
        if(!resultSet.next()) return null;
        ret[1] = resultSet.getString("Nick");
        ret[2] = String.valueOf( resultSet.getInt("COUNT(Nick)") );
        
        // the biggest victim
        statement = conn.prepareStatement("SELECT COUNT(Nick), Nick FROM Titles WHERE Channel = ? GROUP BY Nick ORDER BY COUNT(Nick) DESC LIMIT 1");
        statement.setString(1, channel);
        resultSet = statement.executeQuery();
        if(!resultSet.next()) return null;
        ret[3] = resultSet.getString("Nick");
        ret[4] = String.valueOf( resultSet.getInt("COUNT(Nick)") );
        
        return ret;
	}
	
	/**
	 * A list of the top titlers
	 * In the form:
	 * <pre>
	 * [ 
	 *   5 jerks [name, given title count],
	 *   5 victims [name, received title count]
	 * ]
	 * </pre>  
	 * @param conn
	 * @param channel
	 * @return
	 * @throws SQLException
	 */
	public synchronized List<String[]> getTopSubmitters(Connection conn, String channel) throws SQLException {
		List<String[]> ret = new LinkedList<String[]>();
		
		// submitters
		PreparedStatement sSub = conn.prepareStatement("SELECT COUNT(Submitter), Submitter FROM Titles WHERE Channel = ? GROUP BY Submitter ORDER BY COUNT(Submitter) DESC LIMIT 5");
		sSub.setString(1, channel);
        ResultSet rSub = sSub.executeQuery();

        while(rSub.next()) {
        	String[] sub = new String[2];
        	sub[0] = rSub.getString("Submitter");
        	sub[1] = rSub.getString("COUNT(Submitter)"); 
        	ret.add(sub);
        }
        return ret;
	}
	
	/**
     * A list of the top titlers and victims
     * In the form:
     * <pre>
     * [ 
     *   5 jerks [name, given title count],
     *   5 victims [name, received title count]
     * ]
     * </pre>  
     * @param conn
     * @param channel
     * @return
     * @throws SQLException
     */
	public synchronized List<String[]> getTopVictims(Connection conn, String channel) throws SQLException {
	    List<String[]> ret = new LinkedList<String[]>();
	    
	    // victims
        PreparedStatement sVic = conn.prepareStatement("SELECT COUNT(Nick), Nick FROM Titles WHERE Channel = ? GROUP BY Nick ORDER BY COUNT(Nick) DESC LIMIT 5");
        sVic.setString(1, channel);
        ResultSet rVic = sVic.executeQuery();
        
        while(rVic.next()) {
            String[] vic = new String[2];
            vic[0] = rVic.getString("Nick");
            vic[1] = rVic.getString("COUNT(Nick)");
            ret.add(vic);
        }
        
        return ret;
	}
	
}
