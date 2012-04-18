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

import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

/**
 * @author Ryan Morrison
 */
public class Topic implements Runnable {
    // Variables
    private MessageEvent<PircBotX> event;
    private boolean isRemoving;
    
    /**
     * Class constructor
     * @param event the MessageEvent that triggered this class
     */
    public Topic(MessageEvent<PircBotX> event, boolean isRemoving) {
        this.event = event;
        this.isRemoving = isRemoving;
    }
    
    /**
     * Appends a string to the current channel's topic
     */
    private void appendToTopic() {
        event.getBot().setTopic(event.getChannel(), event.getChannel().getTopic() + " " + event.getMessage().substring(13));
    }
    
    /**
     * Removes a string from the current channel's topic, if the string exists within the current topic
     */
    private void removeFromTopic() {
        String currentTopic = event.getChannel().getTopic();
        String newTopic = currentTopic.replaceFirst(Pattern.quote(event.getMessage().substring(13)), "");
        if(!currentTopic.equals(newTopic)) {
            event.getBot().setTopic(event.getChannel(), newTopic);
        }
    }
    
    /**
     * Method that executes upon thread start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if(isRemoving) removeFromTopic();
        else appendToTopic();
    }
}