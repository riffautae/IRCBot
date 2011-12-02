package us.rddt.IRCBot;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PartEvent;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.sql.Timestamp;

public class SeenHandler implements Runnable {
	private MessageEvent event;
	private PartEvent pEvent;
	private boolean hasParted;
	private String seenUser;
	
	private static volatile Map<String, Date> userList = Collections.synchronizedMap(new HashMap<String, Date>());
	
	public void run() {
		if(!hasParted) {
			searchUser();
			return;
		} else {
			updateSeen();
			return;
		}
	}
	
	public SeenHandler(MessageEvent event) {
		this.event = event;
	}
	
	public SeenHandler(PartEvent pEvent, boolean hasDisconnected) {
		this.pEvent = pEvent;
		this.hasParted = hasDisconnected;
	}
	
	private void searchUser() {
		seenUser = event.getMessage().substring(6).replaceAll("^\\s+", "").replaceAll("\\s+$", "");
		if(seenUser.equals(event.getUser().getNick())) {
			event.respond("What are you doing?");
			return;
		} else if (seenUser.equals(event.getBot().getNick())) {
			event.respond("I don't think you understand how this command works.");
			return;
		} else if (event.getBot().getUsers(event.getChannel()).contains(event.getBot().getUser(seenUser))) {
			event.respond(seenUser + " is currently in the channel.");
			return;
		} else if(seenUser.equals("")) {
			event.respond("I can't see when a user was last here if you don't give me one!");
			return;
		} else {
			Date seenDate = userList.get(seenUser);
			if(seenDate != null) {
				event.respond(seenUser + " was last seen " + toReadableTime(seenDate) + ".");
			} else {
				event.respond("I haven't seen " + seenUser + ".");
			}
		}
	}
	
	private void updateSeen() {
		synchronized(userList) {
			userList.put(pEvent.getUser().getNick(), new Date());
		}
	}
	
	private String toReadableTime(Date date) {
		// Calculate the difference in seconds between the quote's submission and now
		long diffInSeconds = (new Date().getTime() - date.getTime()) / 1000;

		// Calculate the appropriate day/hour/minute/seconds ago values and insert them into a long array
	    long diff[] = new long[] { 0, 0, 0, 0 };
	    diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
	    diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
	    diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
	    diff[0] = (diffInSeconds = (diffInSeconds / 24));
	    
	    // Build the readable format string
	    if(diff[0] != 0) return String.format("about %d day%s ago", diff[0], diff[0] > 1 ? "s" : "");
	    if(diff[1] != 0) return String.format("about %s%s hour%s ago", diff[1] > 1 ? "" : "an", diff[1] > 1 ? String.valueOf(diff[1]) : "", diff[1] > 1 ? "s" : "");
	    if(diff[2] != 0) return String.format("about %d minute%s ago", diff[2], diff[2] > 1 ? "s" : "");
	    if(diff[3] != 0) return "just a moment ago";
	    else return "an unknown time ago";
	}
}
