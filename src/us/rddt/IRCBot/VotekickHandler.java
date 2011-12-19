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

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class VotekickHandler implements Runnable {
	// Variables
	private MessageEvent<PircBotX> event;
	private NickChangeEvent<PircBotX> nickEvent;
	private boolean vetoAttempt = false;

	// We need these variables to be accessible from other threads, so we make it static and volatile
	private static volatile String votekickUser = "";
	private static volatile AtomicInteger requiredVotes = new AtomicInteger(-1);
	private static volatile AtomicInteger timeRemaining = new AtomicInteger(60);
	private static volatile List<String> votedUsers = new ArrayList<String>();
	private static volatile AtomicBoolean voteInProgress = new AtomicBoolean(false);

	// Method that executes upon start of thread
	public void run() {
		if(nickEvent != null) {
			if(votekickUser.equals(nickEvent.getOldNick())) {
				votekickUser = nickEvent.getNewNick().replaceAll("^\\s+", "").replaceAll("\\s+$", "");
			}
			else if(votedUsers.contains(nickEvent.getOldNick())) {
				synchronized(votedUsers) {
					votedUsers.set(votedUsers.indexOf(nickEvent.getOldNick()), nickEvent.getNewNick());
				}
			}
			return;
		}
		// There is no votekick in progress
		if(!voteInProgress.get()) {
			// Set the current votekick user
			try {
				synchronized(votekickUser) {
					votekickUser = event.getMessage().substring(10).replaceAll("^\\s+", "").replaceAll("\\s+$", "");
				}
			} catch (IndexOutOfBoundsException ex) {
				return;
			}
			// Determine the number of required votes to pass
			requiredVotes.set((int)(event.getChannel().getUsers().size() * 0.25));
			// If we don't have enough people to warrant a votekick, then fail it immediately
			if(requiredVotes.get() < 2) {
				event.respond("There are not enough people to start a votekick!");
				resetKick();
				return;
			}
			// Ensure the user we wish to kick exists - if not, fail and reset for the next vote
			if(!event.getBot().getUsers(event.getChannel()).contains(event.getBot().getUser(votekickUser))) {
				event.respond("Cannot votekick user - user doesn't exist!");
				resetKick();
				return;
			}
			// Ensure the vote isn't against the bot - we can't let that happen!
			if(votekickUser.equals(event.getBot().getNick())) {
				event.getBot().sendMessage(event.getChannel(), "I'm sorry " + event.getUser().getNick() + ", but I cannot allow you to do that.");
				resetKick();
				return;
			}
			// Ensure we can actually kick the user - there's no point if we can't
			if(event.getBot().getUser(votekickUser).getChannelsOwnerIn().contains(event.getChannel())) {
				event.respond("Cannot votekick user - it is impossible to kick a channel owner!");
				resetKick();
				return;
			}
			// Add the vote starter as a voted user
			votedUsers.add(event.getUser().getHostmask());
			// Reset the votekick timer (just in case it has not been reset)
			timeRemaining.set(60);
			// Set the AtomicBoolean to reflect a vote in progress
			voteInProgress.set(true);
			// Announce the votekick
			event.getBot().sendMessage(event.getChannel(), event.getUser().getNick() + " has voted to kick " + votekickUser + "! Type !votekick " + votekickUser + " to cast a vote. (" + requiredVotes + " needed)");
			// Start ticking. Votes will reset the tick counter, keeping the vote alive.
			while(timeRemaining.get() > 0) {
				// Ensure our user still exists on the channel, if not cancel the votekick
				if(!event.getBot().getUsers(event.getChannel()).contains(event.getBot().getUser(votekickUser)) && !votekickUser.equals("")) {
					event.getBot().sendMessage(event.getChannel(), "The vote to kick " + votekickUser + " has failed! (" + votekickUser + " has left the channel)");
					resetKick();
					return;
				}
				timeRemaining.decrementAndGet();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					IRCUtils.Log(IRCUtils.LOG_ERROR, ex.getMessage());
					ex.printStackTrace();
				}
			}
			// See if the vote has reached a conclusion. If not, fail and reset the vote.
			if(voteInProgress.get()) {
				event.getBot().sendMessage(event.getChannel(), "The vote to kick " + votekickUser + " has failed! (" + requiredVotes + " more needed)");
				resetKick();
				return;
			}
		}
		// A votekick is in progress and someone has attempted to veto
		else if(vetoAttempt) {
			// Only kings should be able to veto votekicks
			if(KingHandler.isUserKing(event.getUser())) {
				event.getBot().sendMessage(event.getChannel(), "The vote to kick " + votekickUser + " has failed! (" + event.getUser().getNick() + " has vetoed the votekick.)");
				resetKick();
				return;
			} else {
				return;
			}
		}
		// There is a vote in progress and the user has voted to kick
		else if(votekickUser.equals(event.getMessage().substring(10).replaceAll("^\\s+", "").replaceAll("\\s+$", ""))) {
			// Ensure the user isn't trying to vote more than once
			if(hasVoted(event.getUser().getHostmask())) {
				event.respond("You cannot vote more than once!");
				return;
			}
			// One less required vote to pass
			requiredVotes.decrementAndGet();
			// Reset the time remaining to 30 seconds if there's less than 30 seconds remaining to vote
			if(timeRemaining.get() < 30) timeRemaining.set(30);
			// Announce the vote to kick
			event.getBot().sendMessage(event.getChannel(), event.getUser().getNick() + " has voted to kick " + votekickUser + "! (" + requiredVotes + " needed)");
			// Add the user to the voted list
			votedUsers.add(event.getUser().getHostmask());
			// If we don't need any more votes to pass, kick the user and reset the system
			synchronized(requiredVotes) {
				if(requiredVotes.get() <= 0) {
					kickUser();
					return;
				}
			}
		}
		// A votekick is in progress and someone is trying to start a new one
		else {
			event.respond("You cannot vote to kick another user while a votekick is currently in progress.");
			return;
		}
	}

	// Class constructor
	public VotekickHandler(MessageEvent<PircBotX> event) {
		this.event = event;
	}

	// Class constructor for veto attempts
	public VotekickHandler(MessageEvent<PircBotX> event, boolean vetoAttempt) {
		this.event = event;
		this.vetoAttempt = vetoAttempt;
	}

	// Class constructor for nick changes
	public VotekickHandler(NickChangeEvent<PircBotX> event) {
		this.nickEvent = event;
	}

	// Method to reset the votekick system when a vote has finished
	private void resetKick() {
		synchronized(votekickUser) {
			votekickUser = "";
		}
		synchronized(votedUsers) {
			votedUsers = new ArrayList<String>();
		}
		requiredVotes.set(-1);
		timeRemaining.set(60);
		voteInProgress.set(false);
	}

	// Method to check if a user has voted in the votekick
	private boolean hasVoted(String nick) {
		if(votedUsers.contains(nick)) return true;
		else return false;
	}

	// Method called when a vote passes to kick the offending user
	private void kickUser() {
		event.getBot().sendMessage(event.getChannel(), "Vote succeeded - kicking " + votekickUser + "!");
		event.getBot().kick(event.getChannel(), event.getBot().getUser(votekickUser), "You have been voted out of the channel!");
		IRCUtils.Log(IRCUtils.LOG_INFORMATION, votekickUser + " has been kicked from the channel by a votekick.");
		resetKick();
	}

}
