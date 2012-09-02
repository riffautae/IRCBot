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

import java.io.IOException;
import java.util.logging.Level;

import org.json.JSONException;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import us.rddt.IRCBot.Configuration;
import us.rddt.IRCBot.IRCUtils;
import us.rddt.IRCBot.Implementations.UrbanLookup;

/**
 * Uses the Urban Dictionary to define words or phrases passed to the bot via a command.
 * 
 * @author Ryan Morrison
 */
public class Define implements Runnable {
    /*
     * Class variables
     */
    private MessageEvent<PircBotX> event;
    
    /**
     * Class constructor
     * @param event the MessageEvent that triggered this class
     */
    public Define(MessageEvent<PircBotX> event) {
        this.event = event;
    }
    
    /**
     * Formats a lookup into a string that can be inserted into a URL
     * @param phrase the phrase(s) to lookup in array form
     * @return the properly formatted string
     * @throws ArrayIndexOutOfBoundsException if the user did not define a phrase to look up
     */
    private String formatLookup(String[] phrase) throws ArrayIndexOutOfBoundsException {
        String temp = "";
        for(int i = 1; i < phrase.length; i++) {
            if(i == (phrase.length - 1)) temp += phrase[i];
            else temp += phrase[i] + "%20";
        }
        return temp;
    }
    
    /**
     * Method that executes upon thread-start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        /*
         * Variables
         */
        UrbanLookup lookupResult = null;
        String toDefine = null;
        
        /*
         * Attempts to extract the phrase to define from the user's message
         */
        try {
            toDefine = formatLookup(event.getMessage().split(" "));
        } catch (ArrayIndexOutOfBoundsException ex) {
            return;
        }
        
        /*
         * Attempts to define the phrase via UrbanDictionary. If an exception occurs,
         * return a proper error message.
         */
        try {
            lookupResult = UrbanLookup.getDefinition(toDefine);
        } catch (IOException ex) {
            Configuration.getLogger().write(Level.WARNING, IRCUtils.getStackTraceString(ex));
            event.respond("Error while downloading definition: " + IRCUtils.trimString(event.getMessage(), 50));
            return;
        } catch (JSONException ex) {
            Configuration.getLogger().write(Level.WARNING, IRCUtils.getStackTraceString(ex));
            event.respond("Error while parsing definition: " + IRCUtils.trimString(event.getMessage(), 50));
        }
        
        /*
         * Return the result to the user based upon whether the lookup was successful or not.
         */
        if(lookupResult.hasResult()) {
            event.respond(lookupResult.getWord() + ": " + lookupResult.getDefinition() + " (Example: " + lookupResult.getExample() + ")");
        } else {
            event.respond("The definition for " + toDefine + " does not exist.");
        }
    }
}
