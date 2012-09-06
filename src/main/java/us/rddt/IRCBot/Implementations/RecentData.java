package us.rddt.IRCBot.Implementations;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Per channel info for recent announcements 
 * (To avoid flooding on connection issues.)
 * NOT thread safe, sync all calls
 * 
 * @author Milton Thomas
 */
public class RecentData {
	
    /*
     * Class variables.
     */

	// rate limiting
	private final double rate = 3.0; // messages
	private final double per = 10.0; // seconds
	
	private double allowance = rate;
	private Date lastCheck = new Date();
	
	// Last time the user got announced
    private Map<String, Date> userLast = new HashMap<String, Date>();
    
    public final static int MAX_HISTORY = 3;
    
    // list of titles
    private LinkedList<RecentAnnouncement> history = new LinkedList<RecentAnnouncement>();
    
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
    	history.addLast(new RecentAnnouncement(titleId, nick, title));
    	if (history.size() == MAX_HISTORY)
    		history.pop();
    }
    
    /**
     * @return returns a copy of the history
     */
    public LinkedList<RecentAnnouncement> listHistory() {
    	return new LinkedList<RecentAnnouncement>(history);
    }
    
    public RecentAnnouncement last() {
    	return history.peek();
    }
}