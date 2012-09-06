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

import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import us.rddt.IRCBot.Implementations.GoogleResult;
import us.rddt.IRCBot.Implementations.GoogleSearch;

/**
 * Searches Google for a provided query string and returns the result to the channel.
 * 
 * @author Ryan Morrison
 */
public class Search implements Runnable {
    /*
     * Class variables
     */
    private MessageEvent<PircBotX> event;
    private List<Object> result;
    private StringBuilder resultText = new StringBuilder();
    private List<GoogleResult> searchResults;
    
    /**
     * Class constructor
     * @param event the MessageEvent that triggered this class
     */
    public Search(MessageEvent<PircBotX> event) {
        this.event = event;
    }
    
    /**
     * Method that executes upon thread start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @SuppressWarnings("unchecked")
    public void run() {
        try {
            // Retrieves the search results
            result = GoogleSearch.performSearch(event.getMessage().substring(3));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // The second value in the ArrayList should contain our list of results
        searchResults = (List<GoogleResult>)result.get(1);
        for(GoogleResult gr : searchResults) {
            resultText.append(gr.getTitle() + ": " + gr.getUrl() + " | ");
        }
        resultText.append("+" + result.get(0) + " more results");
        event.respond(resultText.toString());
    }
}
