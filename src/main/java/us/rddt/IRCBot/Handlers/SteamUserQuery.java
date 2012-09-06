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

import java.text.SimpleDateFormat;
import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import us.rddt.IRCBot.Configuration;
import us.rddt.IRCBot.IRCUtils;

import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;
import com.github.koraktor.steamcondenser.steam.community.SteamId;

/**
 * Queries Valve's Steam service for the status of a given user.
 * 
 * @author Ryan Morrison
 */
public class SteamUserQuery implements Runnable {
    // Variables
    private MessageEvent<PircBotX> event;

    /**
     * Class constructor
     * @param event the MessageEvent that triggered this class
     */
    public SteamUserQuery(MessageEvent<PircBotX> event) {
        this.event = event;
    }
    
    /**
     * Performs a query on a user and returns basic details
     * @param user the Steam user string to query
     * @throws SteamCondenserException if the user's profile cannot be loaded
     */
    private void doUserQuery(String user) throws SteamCondenserException {
        // Query Steam for the user
        SteamId userId = SteamId.create(user);
        
        // Convert the date since the user's creation into a formatted string
        String memberSince = new SimpleDateFormat("dd/MM/yyyy").format(userId.getMemberSince());
        
        // Build the string to return to the user
        StringBuilder builtResponse = new StringBuilder();
        builtResponse.append("User " + userId.getNickname() + ", member since " + memberSince + ". " + userId.getStateMessage());
        
        // Return the string to the user
        event.respond(builtResponse.toString());
    }
    
    /**
     * Method that executes upon thread start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            // Retrieve the user's name to look up from the received message and query it
            doUserQuery(event.getMessage().split(" ")[1]);
        } catch (Exception ex) {
            event.respond("Could not load profile: " + ex.getMessage());
            Configuration.getLogger().write(Level.WARNING, IRCUtils.getStackTraceString(ex));
        }
    }
}
