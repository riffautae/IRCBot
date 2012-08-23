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

package us.rddt.IRCBot.Implementations;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.User;

/**
 * @author Ryan Morrison
 */
public class VotekickObject {
    // Class variables
    private User user;
    private int numVotes;
    private int numVotesRequired;
    private List<String> votedUsers = new ArrayList<String>();
    
    /**
     * Class constructor
     * @param user the user being votekicked
     * @param startingUser the user who started the votekick
     * @param numVotesRequired the number of votes required for the vote to pass
     */
    public VotekickObject(User user, User startingUser, int numVotesRequired) {
        this.user = user;
        this.numVotes = 0;
        this.numVotesRequired = numVotesRequired;
        votedUsers.add(startingUser.getHostmask());
    }
    
    /**
     * Adds a vote against the user
     */
    public void addVote() {
        numVotes++;
    }
    
    /**
     * Adds the user who voted to the voted users' array
     * @param user the user to add
     */
    public void addVotedUser(User user) {
        votedUsers.add(user.getHostmask());
    }

    /**
     * Returns the number of current votes against the user
     * @return the number of current votes against the user
     */
    public int getNumVotes() {
        return numVotes;
    }

    /**
     * Returns the number of votes required to kick
     * @return the number of votes required to kick
     */
    public int getNumVotesRequired() {
        return numVotesRequired;
    }
    
    /**
     * Returns the users who voted in the votekick
     * @return the users who voted in the votekick
     */
    public List<String> getVotedUsers() {
        return votedUsers;
    }
    
    /**
     * Returns the User object
     * @return the User object
     */
    public User getUser() {
        return user;
    }
    
    /**
     * Returns if there are enough votes for the votekick to pass
     * @return true if the votekick has passed, false if it has not
     */
    public boolean hasNeededVotes() {
        return numVotes >= numVotesRequired;
    }
}
