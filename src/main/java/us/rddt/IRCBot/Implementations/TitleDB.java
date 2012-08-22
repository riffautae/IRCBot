package us.rddt.IRCBot.Implementations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.pircbotx.Channel;
import org.pircbotx.User;

import us.rddt.IRCBot.IRCUtils;

/**
 * Per channel info for recent announcements 
 * (To avoid flooding on connection issues.)
 * NOT thread safe, sync all calls
 * 
 * @author Milton Thomas
 */
class RecentData {
	
    /*
     * Class variables.
     */

	// rate limiting
	private final double rate = 3.0; // messages
	private final double per = 10.0; // milliseconds
	
	private double allowance = rate;
	private Date lastCheck = new Date();
	
	// Last time the user got announced
    private Map<String, Date> userLast = new HashMap<String, Date>();
    
    private final static int MAX_HISTORY = 3;
    
    // list of titles
    private LinkedList<RecentAnnounces> history = new LinkedList<RecentAnnounces>();
    
 	// Data for monologue checking
    private String lastUser;
    private int monoOften;
    
    /**
     * Call to see if we should make an announcement
     * @param id 'Unique' user id
     * @return true if user joining isnt spammy
     */
    public boolean checkAnnounce(String id) {
		Date current = new Date();
		long timePassed = (current.getTime() - lastCheck.getTime())/1000;
		lastCheck = current;
		allowance += timePassed * (rate / per);
		
		if (allowance > rate)
			allowance = rate; // throttle
		
		// skip if user was announced recently
		Date userLastCheck = userLast.get(id);
		userLast.put(id, current);
		
		if (userLastCheck != null) {
			if ( 30000 < (current.getTime() - userLastCheck.getTime()) )
				return false;
		}
		
		// skip if there is too much announcing going on
		if (allowance < 1.0) {
			return false;
		} else {
			allowance -= 1.0;
			cleanup(); // thanks to rate limiting and such this wont run often
		    return true;
		}
    }
    
    /**
     * Check if this user is monologing
     * @param id the user
     * @return
     */
    public boolean inMonologue(String id) {
    	if (lastUser == id) {
    		monoOften++;
    		if( monoOften >= 4) return true;
    	} else {
    		lastUser = id;
    		monoOften = 0;
    	}
    	return false;
    }
    
    // cull really old ids
    private void cleanup() {
    	List<String> toRemove = new LinkedList<String>();
    	Date current = new Date();
    	for(String id : userLast.keySet()) {
    		if( 30000 > (current.getTime() - userLast.get(id).getTime()) )
    			toRemove.add(id);
    	}
    	
    	for(String id : toRemove)
			userLast.remove(id);
    }
    
    public void addHistory(int titleId, String nick, String title) {
    	history.addLast(new RecentAnnounces(titleId, nick, title));
    	if (history.size() == MAX_HISTORY)
    		history.pop();
    }
    
    /**
     * @return returns a copy of the history
     */
    public LinkedList<RecentAnnounces> listHistory() {
    	return new LinkedList<RecentAnnounces>(history);
    }
}

/**
 * Keep a history of announces as well as do throttling
 * @author Milton Thomas
 *
 */
class RecentAnnounces {
	private String user;
	private int titleId;
	private String title;
	
	public RecentAnnounces(int titleId, String user, String title) {
		this.user = user;
		this.titleId = titleId;
		this.title = title;
	}

	public String getUser() {
		return user;
	}

	public int getTitleId() {
		return titleId;
	}

	public String getTitle() {
		return title;
	}
}

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
	
	private static Random rand = new Random();
	
	// A recentData for each channel
	private Map<String, RecentData> recentTitles = new ConcurrentHashMap<String, RecentData>();

	
	// victim/submitter aliases for funzies
	private static final String[][] VIC_SUB_ALIAS = new String[][]
			{
				{"biggest jerk", "tiniest baby"},
				{"best attacker", "worst defender"},
				{"rock", "paper"},
				{"red medic", "blue sniper"},
				{"top submitter", "top receiver"},
				{"best giver", "best receiver"},
				{":t", ":ok"},
				{"spammer", "collector"},
			};

	private String[] getVicSubAlias() {
		int index = rand.nextInt(VIC_SUB_ALIAS.length-1);
		return VIC_SUB_ALIAS[index];
	}
	
	/**
	 * 
	 * @param conn Db connection
	 * @param chan channel this took place in
	 * @param user who joined
	 * @return The title to say or null if none
	 * @throws SQLException
	 */
	public synchronized String join(Connection conn, Channel chan, User user) throws SQLException {
		String userName = user.getNick();
		String chanName = chan.getName();
		RecentData recent = getRecent(chanName);

		if (recent.checkAnnounce(userName))
			return getTitle(conn, chanName, userName);
		return null;
	}

	/**
	 * Sometimes make fun of people that are monologing
	 * Its a monolog if they are the only speaker at least 5 lines
	 * @param conn Db connection
	 * @param chan
	 * @param user
	 * @return The title to say or null if none
	 * @throws SQLException 
	 */
	
	public synchronized String talk(Connection conn, Channel chan, User user) throws SQLException {
		String userName = user.getNick();
		String chanName = chan.getName();
		RecentData recent = getRecent(chanName);

		// 1 in 20 chance when someone is monologing that we make fun of them
		// this means we should get one title about every 4 short monologues
		if (rand.nextInt(20) == 0 && recent.checkAnnounce(userName))
			return getTitle(conn, chanName, userName);
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
				"INSERT INTO Titles(Submitter, Date, Nick, Title) Values (?,?,?,?)");
		statement.setString(1, submitter);
		statement.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
		statement.setString(3, victim);
		statement.setString(3, title);
		
		return statement.executeUpdate();
	}
	
	public synchronized int remTitleByString(Connection conn, String chan, String nick, String title) throws SQLException {
		PreparedStatement statement = conn.prepareStatement(
				"DELETE FROM Titles WHERE Channel = ? AND Nick = ? AND Title = ?");
		
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
	
	public synchronized List<String> listTitles(Connection conn, String channel, String of, int page) throws SQLException {
		final int PAGE_LENGTH = 4;
		PreparedStatement statement = conn.prepareStatement(
				"SELECT Id, Title FROM Titles WHERE Channel = ? AND User = ? LIMIT ?, ?");
		statement.setString(1, channel);
		statement.setString(2, of);
		statement.setInt(3, page * PAGE_LENGTH + 1);
		statement.setInt(4, PAGE_LENGTH);
		
		ResultSet rs = statement.executeQuery();
		
		LinkedList<String> resp = new LinkedList<String>();
		while (rs.next()) {
			resp.add(rs.getInt("Id") + ") " + rs.getString("Title") );
		}
		return resp;
	}
	
	public synchronized List<String> lastTitles(Connection conn, String channel) {
		RecentData rd = recentTitles.get(channel);
		List<String> resp = new LinkedList<String>();
		for( RecentAnnounces ra : rd.listHistory() ) {
			resp.add(ra.getTitleId() + ") " + ra.getUser() + "> " + ra.getTitle());
		}
		return resp;
	}
	
	public synchronized String getStats(Connection conn, String channel) throws SQLException { 
		// First query to pull the total number of titles
        PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM Titles WHERE Channel = ?");
        statement.setString(1, channel);
        ResultSet resultSet = statement.executeQuery();
        if(!resultSet.next()) return null;
        int count = resultSet.getInt("COUNT(*)");
        
        // the biggest jerk
        statement = conn.prepareStatement("SELECT COUNT(Submitter), Submitter FROM Titles WHERE Channel = ? GROUP BY Nick ORDER BY COUNT(Nick) DESC LIMIT 1");
        statement.setString(1, channel);
        resultSet = statement.executeQuery();
        if(!resultSet.next()) return null;
        String jerkName = resultSet.getString("Nick");
        int jerkNum = resultSet.getInt("COUNT(Nick)");
        
        // the biggest victim
        statement = conn.prepareStatement("SELECT COUNT(Submitter), Submitter FROM Titles WHERE Channel = ? GROUP BY Nick ORDER BY COUNT(Nick) DESC LIMIT 1");
        statement.setString(1, channel);
        resultSet = statement.executeQuery();
        if(!resultSet.next()) return null;
        String victimName = resultSet.getString("Nick");
        int victimNum = resultSet.getInt("COUNT(Nick)");
        
        String[] alias = getVicSubAlias();
        
        return "I have " + count + " titles. The " + alias[0] + ": " + jerkName + " (" + jerkNum + "). " + 
        	"The " + alias[1] + ": " + victimName + " (" + victimNum + ").";
	}
	
	public synchronized String getTop(Connection conn, String channel) throws SQLException {
		int tempCount = 1;

		// submitters
		PreparedStatement sSub = conn.prepareStatement("SELECT COUNT(Submitter), Submitter FROM Titles WHERE Channel = ? GROUP BY Submitter ORDER BY COUNT(Submitter) DESC LIMIT 5");
		sSub.setString(1, channel);
        ResultSet rSub = sSub.executeQuery();
        
        List<String> subs = new LinkedList<String>();

        while(rSub.next()) {
        	subs.add(tempCount + ": " + rSub.getString("Nick") + " (" + rSub.getInt("COUNT(Nick)") + "), ");
            tempCount++;
        }
        
        // victims
        PreparedStatement sVic = conn.prepareStatement("SELECT COUNT(Nick), Nick FROM Titles WHERE Channel = ? GROUP BY Nick ORDER BY COUNT(Nick) DESC LIMIT 5");
        sVic.setString(1, channel);
        ResultSet rVic = sVic.executeQuery();
        
        List<String> victims = new LinkedList<String>();
        while(rVic.next()) {
        	victims.add(tempCount + ": " + rVic.getString("Nick") + " (" + rVic.getInt("COUNT(Nick)") + "), ");
            tempCount++;
        }
        
        String[] alias = getVicSubAlias();
        
        return "The " + alias[0] + "'s: " + IRCUtils.join(subs, ", ") + 
        		". The " + alias[1] + "'s" + IRCUtils.join(victims, ", ");
	}
	
	// --- helpers ---

	private String getTitle(Connection conn, String chan, String nick) throws SQLException {
		
		String title = null;
		
		PreparedStatement sCount = conn.prepareStatement(
				"SELECT COUNT(Nick) FROM Titles WHERE Channel = ? AND Nick = ?");
		sCount.setString(1, chan);
		sCount.setString(2, nick);

		ResultSet result = sCount.executeQuery();
		if (result.next()) {
        	int count = result.getInt(1);
        	int item = rand.nextInt(count) + 1;
        	
			PreparedStatement statement = conn.prepareStatement(
					"SELECT Title FROM Titles WHERE Nick = ? LIMIT ?, 1");
	        statement.setString(1, nick);
	        statement.setInt(2, item);
	        
	        result = statement.executeQuery();
	        if (result.next())
	        	title = result.getString(1);
		} 
        return title;
	}
	
	private RecentData getRecent(String chanName) {
		RecentData recent;
		if (recentTitles.containsKey(chanName)) {
			recent = recentTitles.get(chanName);
		} else {
			recent = new RecentData();
			recentTitles.put(chanName, recent);
		}
		return recent;
	}
}
