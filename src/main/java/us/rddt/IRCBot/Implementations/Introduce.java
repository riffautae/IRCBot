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

import us.rddt.IRCBot.Database;


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
	private Date last_check = new Date();
	
	// Last time the user got announced
    private Map<String, Date> user_last = new HashMap<String, Date>();
    
 	// Data for monologue checking
    private String last_user;
    private int mono_often;
    
    /**
     * Call to see if we should make an announcement
     * @param id 'Unique' user id
     * @return true if user joining isnt spammy
     */
    public boolean check_announce(String id) {
		Date current = new Date();
		long time_passed = (current.getTime() - last_check.getTime())/1000;
		last_check = current;
		allowance += time_passed * (rate / per);
		
		if (allowance > rate)
			allowance = rate; // throttle
		
		// skip if user was announced recently
		Date user_last_check = user_last.get(id);
		user_last.put(id, current);
		
		if (user_last_check != null) {
			if ( 30000 < (current.getTime() - user_last_check.getTime()) )
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
    public boolean in_monologue(String id) {
    	if (last_user == id) {
    		mono_often++;
    		if( mono_often >= 4) return true;
    	} else {
    		last_user = id;
    		mono_often = 0;
    	}
    	return false;
    }
    
    // cull really old ids
    private void cleanup() {
    	List<String> to_remove = new LinkedList<String>();
    	Date current = new Date();
    	for(String id : user_last.keySet()) {
    		if( 30000 > (current.getTime() - user_last.get(id).getTime()) )
    			to_remove.add(id);
    	}
    	
    	for(String id : to_remove)
			user_last.remove(id);
    }
}

/**
 * @author Milton Thomas
 */
public class Introduce {
	
	/* announce:
	 * on join or too much monologue 
	 * 	get random intro
	 */
	
	/*
	 * control:
 	 * on pm
	 *  for user
	 *   add intro
	 *   list intro[s]
	 *   rem intro
	 */
	
	// singleton time
	private static Introduce _intro = new Introduce();
	
	public static Introduce instance_of() {
		return _intro;
	}
	
	private Random rand = new Random();
	
	// A recentData for each channel
	private Map<String, RecentData> recent_intros = new ConcurrentHashMap<String, RecentData>();
	
	/**
	 * 
	 * @param conn Db connection
	 * @param chan channel this took place in
	 * @param user who joined
	 * @return The intro to say or null if none
	 * @throws SQLException
	 */
	public synchronized String join(Connection conn, Channel chan, User user) throws SQLException {
		String user_name = user.getNick();
		String chan_name = chan.getName();
		RecentData recent = getRecent(chan_name);

		if (recent.check_announce(user_name))
			return getIntro(conn, user_name);
		return null;
	}

	/**
	 * Sometimes make fun of people that are monologing
	 * Its a monolog if they are the only speaker at least 5 lines
	 * @param conn Db connection
	 * @param chan
	 * @param user
	 * @return The intro to say or null if none
	 * @throws SQLException 
	 */
	
	public synchronized String talk(Connection conn, Channel chan, User user) throws SQLException {
		String user_name = user.getNick();
		String chan_name = chan.getName();
		RecentData recent = getRecent(chan_name);

		// 1 in 50 chance when someone is monologing that we make fun of them
		// this means we should get one intro about every 10 short monologues
		if (rand.nextInt(50) == 0 && recent.check_announce(user_name))
			return getIntro(conn, user_name);
		return null;
	}
	
	public synchronized String add_intro(Connection conn, User user, String to, String intro) throws SQLException {
		String submitter = user.getNick();
		
		if (submitter == to)
			return "NO THAT IS AGAINST THE SPIRIT OF THE GAME!";
		
		PreparedStatement statement = conn.prepareStatement(
				"INSERT INTO Intros(Submitter, Date, Nick, Intro) Values (?,?,?,?)");
		statement.setString(1, submitter);
		statement.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
		statement.setString(3, to);
		statement.setString(3, intro);
		
		statement.executeUpdate();
		
		return "Added.";
	}
	
	public synchronized void rem_intro(Connection conn, User user, String intro_id) {
		
	}
	
	public synchronized void list_intros(Connection conn, User user, String of) {
		
	}
	
	public synchronized void last_intro(Connection conn, User user, String of) {
		
	}
	
	// --- helpers ---

	private String getIntro(Connection conn, String nick) throws SQLException {
		
		String intro = null;
		
		PreparedStatement s_count = conn.prepareStatement(
				"SELECT COUNT(Nick) FROM Intros WHERE Nick = ?");
		s_count.setString(1, nick);

		ResultSet result = s_count.executeQuery();
		if (result.next()) {
        	int count = result.getInt(1);
        	int item = rand.nextInt(count) + 1;
        	
			PreparedStatement statement = conn.prepareStatement(
					"SELECT Intro FROM Intros WHERE Nick = ? LIMIT ?, 1");
	        statement.setString(1, nick);
	        statement.setInt(2, item);
	        
	        result = statement.executeQuery();
	        if (result.next())
	        	intro = result.getString(1);
		} 
        return intro;
	}
	
	private RecentData getRecent(String chan_name) {
		RecentData recent;
		if (recent_intros.containsKey(chan_name)) {
			recent = recent_intros.get(chan_name);
		} else {
			recent = new RecentData();
			recent_intros.put(chan_name, recent);
		}
		return recent;
	}
}
