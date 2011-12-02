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

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SeenHandler implements Runnable {
	// Variables
	private MessageEvent event;
	private PartEvent pEvent;
	private QuitEvent qEvent;
	private boolean hasParted;
	private String seenUser;
	
	// We need this to be accessible from other threads, so we make it static and volatile
	// This will hold our list of previous users.
	private static volatile Map<String, Date> userList = Collections.synchronizedMap(new HashMap<String, Date>());
	
	// Method that executes upon start of thread
	public void run() {
		// We've received a direct !seen command
		if(!hasParted) {
			searchUser();
			return;
		}
		// We've received a notification that a user has left a channel
		else {
			updateSeen();
			return;
		}
	}
	
	// Class constructor
	public SeenHandler(MessageEvent event) {
		this.event = event;
	}
	
	// Overloaded class constructor used for parting events
	public SeenHandler(PartEvent pEvent) {
		this.pEvent = pEvent;
		this.hasParted = true;
	}
	
	// Overloaded class constructor used for parting events
	public SeenHandler(QuitEvent event) {
		this.qEvent = event;
		this.hasParted = true;
	}
	
	// Method to search the key/value store for a user
	private void searchUser() {
		// Extract the user name from the command and remove any unnecessary whitespace
		seenUser = event.getMessage().substring(6).replaceAll("^\\s+", "").replaceAll("\\s+$", "");
		// The user is performing the command on themselves?
		if(seenUser.equals(event.getUser().getNick())) {
			event.respond("What are you doing?");
			return;
		// The user is performing the command on the bot?
		} else if (seenUser.equals(event.getBot().getNick())) {
			event.respond("I don't think you understand how this command works.");
			return;
		// Make sure the user isn't in the channel, if they are then just return that they are
		} else if (event.getBot().getUsers(event.getChannel()).contains(event.getBot().getUser(seenUser))) {
			event.respond(seenUser + " is currently in the channel.");
			return;
		// Make sure we don't have a blank request
		} else if(seenUser.equals("")) {
			event.respond("I can't see when a user was last here if you don't give me one!");
			return;
		// If all else fails, we have a valid request
		} else {
			// Get the date that the user was last seen and send it back. If there's no date, we never saw the user.
			Date seenDate = userList.get(seenUser);
			if(seenDate != null) {
				event.respond(seenUser + " was last seen " + toReadableTime(seenDate) + ".");
			} else {
				event.respond("I haven't seen " + seenUser + ".");
			}
		}
	}
	
	// Method called when a user parts/quits. Update their key/value with the timestamp of when they left.
	private void updateSeen() {
		synchronized(userList) {
			if(pEvent != null) userList.put(pEvent.getUser().getNick(), new Date());
			else userList.put(qEvent.getUser().getNick(), new Date());
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
