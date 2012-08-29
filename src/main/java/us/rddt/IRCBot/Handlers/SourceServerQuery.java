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

import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import us.rddt.IRCBot.Configuration;
import us.rddt.IRCBot.IRCUtils;

import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;
import com.github.koraktor.steamcondenser.steam.servers.SourceServer;

/**
 * @author Ryan Morrison
 */
public class SourceServerQuery implements Runnable {
    // Variables
    private MessageEvent<PircBotX> event;

    /**
     * Class constructor
     * @param event the MessageEvent that triggered this class
     */
    public SourceServerQuery(MessageEvent<PircBotX> event) {
        this.event = event;
    }
    
    /**
     * Queries a Source engine server and returns basic information about it to the client
     * @param ip The IP address of the server
     * @param port The port of the server
     * @throws SteamCondenserException if the server returns in an undefined manner
     * @throws TimeoutException if the connection attempt to the server times out
     */
    private void doSourceQuery(String ip, int port) throws SteamCondenserException, TimeoutException {
        // Perform the query against the server
        SourceServer sourceServer = new SourceServer(ip, port);
        sourceServer.initialize();
        
        // Create variables for the information we want
        String serverName = (String) sourceServer.getServerInfo().get("serverName");
        String gameDescription = (String) sourceServer.getServerInfo().get("gameDescription");
        String serverMap = (String) sourceServer.getServerInfo().get("mapName");
        int numPlayers = (Integer) sourceServer.getServerInfo().get("numberOfPlayers");
        int numBots = (Integer) sourceServer.getServerInfo().get("numberOfBots");
        int maxNumPlayers = (Integer) sourceServer.getServerInfo().get("maxPlayers");
        boolean isLocked = (Boolean) sourceServer.getServerInfo().get("passwordProtected");
        
        // Bots do show up as regular players, subtract the number of bots to get the number of actual players
        if(numBots > 0) numPlayers = numPlayers - numBots;
        
        // Build the string to return to the user
        StringBuilder builtResponse = new StringBuilder();
        builtResponse.append(serverName + " playing "+ gameDescription +" on map " + serverMap + " with ");
        if(numBots > 0) builtResponse.append(numPlayers + "/" + maxNumPlayers + " players (" + numBots + " bots)");
        else builtResponse.append(numPlayers + "/" + maxNumPlayers + " players");
        if(isLocked) builtResponse.append(Colors.RED + "[Locked] " + Colors.NORMAL);
        
        // Return the string to the user
        event.respond(builtResponse.toString());
    }
    
    /**
     * Method that executes upon thread start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        // Retrieve the IP address of the server to query and split it into the IP address and port
        String[] address = event.getMessage().split(" ")[1].split(":");
        // Attempt to query or return an error if it fails
        try {
            if(address.length > 2) doSourceQuery(address[0], Integer.parseInt(address[1]));
            else doSourceQuery(address[0], 27015);
        } catch (Exception ex) {
            event.respond("Could not query the server - it may be offline, not a Source engine game or an incorrect IP address or port has been provided");
            Configuration.getLogger().write(Level.WARNING, IRCUtils.getStackTraceString(ex));
        }
    }
}
