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

import java.util.HashMap;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

import us.rddt.IRCBot.Configuration;
import us.rddt.IRCBot.Implementations.VotekickObject;

/**
 * Allows any user to start a votekick against another user in the channel. Others
 * are encouraged to participate if they choose to, and should the user receive the
 * required number of votes for the votekick to pass, they are kicked from the channel.
 * 
 * @author Ryan Morrison
 */
public class Votekick implements Runnable {
    // Variables
    private MessageEvent<PircBotX> event;
    private static volatile HashMap<Channel,VotekickObject> currVotekicks = new HashMap<Channel,VotekickObject>();

    /**
     * Class constructor
     * @param event the MessageEvent that triggered this class
     */
    public Votekick(MessageEvent<PircBotX> event) {
        this.event = event;
    }

    /**
     * Adds a vote against the user
     * @param user the user voting
     */
    private void addVote(User user) {
        getVotekickObject().addVote();
        getVotekickObject().addVotedUser(user);
    }

    /**
     * Kicks the user from the channel and ends the votekick
     * @param channel the channel to apply the kick to
     * @param user the user to kick
     */
    private void kickUser(Channel channel, User user) {
        event.getBot().kick(channel, user);
        synchronized(currVotekicks) {
            currVotekicks.remove(channel);
        }
    }

    /**
     * Ends the votekick in a channel
     * @param channel the channel to end the votekick in
     */
    private void finishVote(Channel channel) {
        synchronized(currVotekicks) {
            currVotekicks.remove(channel);
        }
    }

    /**
     * Returns the votekick object
     * @return the votekick object
     */
    private VotekickObject getVotekickObject() {
        return currVotekicks.get(event.getChannel());
    }

    /**
     * Returns whether a votekick is in progress or not
     * @param channel the channel to check
     * @return true if a votekick is in progress, false if it is not
     */
    private boolean isVoteInProgress(Channel channel) {
        if(currVotekicks.containsKey(channel)) return true;
        else return false;
    }

    /**
     * Starts a new votekick against a given channel/user
     * @param channel the channel to start the votekick in
     * @param user the user to votekick
     */
    private void startNewVotekick(Channel channel, User startingUser, User votekickUser) {
        // Determine the number of votes required to pass the votekick
        int numVotesRequired = (int)(channel.getUsers().size() * ((double)Configuration.getVotekickPassPercent() / 100));
        // Create the VotekickObject and announce the vote
        synchronized(currVotekicks) {
            currVotekicks.put(channel, new VotekickObject(votekickUser, startingUser, numVotesRequired));
        }
        event.getBot().sendMessage(channel, startingUser.getNick() + " has voted to kick " + getVotekickObject().getUser().getNick() + "! (" + getVotekickObject().getNumVotes() + "/" + getVotekickObject().getNumVotesRequired() + " needed, " + Configuration.getVotekickDuration() + " seconds remaining)");
        /*
         * Start sleeping the thread. When the vote is halfway complete, if it hasn't passed yet, announce the amount
         * of time remaining and how many votes are still needed.
         */
        try {
            Thread.sleep((int)(Configuration.getVotekickDuration() / 2) * 1000);
        } catch (InterruptedException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        if(isVoteInProgress(channel)) {
            event.getBot().sendMessage(channel, "There are " + (int)(Configuration.getVotekickDuration() / 2) + " seconds remaining in the vote to kick " + getVotekickObject().getUser().getNick() + ". (" + getVotekickObject().getNumVotes() + " votes, " + getVotekickObject().getNumVotesRequired() + " needed)");
        }
        /*
         * Sleep again. When the vote time has passed, if the vote is still in progress, then end the vote
         * as a failure.
         */
        try {
            Thread.sleep((int)(Configuration.getVotekickDuration() / 2) * 1000);
        } catch (InterruptedException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        if(isVoteInProgress(channel)) {
            event.getBot().sendMessage(channel, "The vote to kick " + getVotekickObject().getUser().getNick() + " has failed! (" + getVotekickObject().getNumVotes() + " votes, " + getVotekickObject().getNumVotesRequired() + " needed)");
            finishVote(event.getChannel());
        }
    }

    /**
     * Method that executes upon thread start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        // Split the message into parameters
        String[] parameters = event.getMessage().split(" ");
        // Votekick commands always take at least one argument
        if(parameters.length > 1) {
            // Make sure the user isn't trying to apply it to themselves or the bot
            if(parameters[1].equals(event.getUser().getNick())) {
                event.respond("You cannot participate in a votekick against yourself!");
            } else if(parameters[1].equals(event.getBot().getNick())) {
                event.respond("You cannot votekick the bot!");
            } else {
                // If there is already a vote in progress
                if(isVoteInProgress(event.getChannel())) {
                    // Make sure the user hasn't already voted
                    if(getVotekickObject().getVotedUsers().contains(event.getUser().getHostmask())) {
                        event.respond("You have already voted - you cannot vote again!");
                    } else {
                        // Make sure the user is voting against the votekicked user, otherwise tell them to wait until the vote is over
                        if(parameters[1].equals(getVotekickObject().getUser().getNick())) {
                            addVote(event.getUser());
                            event.getBot().sendMessage(event.getChannel(), event.getUser().getNick() + " has voted to kick " + getVotekickObject().getUser().getNick() + "! (" + getVotekickObject().getNumVotes() + "/" + getVotekickObject().getNumVotesRequired() + " needed)");
                            // If there are enough votes for the votekick to pass, kick the user and reset the votekick
                            if(getVotekickObject().hasNeededVotes()) {
                                event.getBot().sendMessage(event.getChannel(), "The votekick against " + getVotekickObject().getUser().getNick() + " has succeeded!");
                                kickUser(event.getChannel(), getVotekickObject().getUser());
                            }
                        } else {
                            event.respond("You cannot start another votekick when one is currently in progress!");
                        }
                    }
                } else {
                    // Make sure the user to votekick actually exists in the channel
                    if(event.getBot().getUsers(event.getChannel()).contains(event.getBot().getUser(parameters[1]))) {
                        startNewVotekick(event.getChannel(), event.getUser(), event.getBot().getUser(parameters[1]));
                    } else {
                        event.respond("You cannot start a vote against a user that is not in the channel!");
                    }
                }
            }
        }
    }
}
