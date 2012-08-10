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

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Event;
import java.util.logging.Level;

import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import us.rddt.IRCBot.Configuration;
import us.rddt.IRCBot.Database;
import us.rddt.IRCBot.Enums.IntrosType;
import us.rddt.IRCBot.Implementations.Introduce;


/**
 * @author Milton Thomas
 */
public class Intros implements Runnable {
	private Event<PircBotX> event;
	private IntrosType type;
	
	public Intros(Event<PircBotX> event, IntrosType type) {
		this.event = event;
		this.type = type;
	}
    /**
     * Method that executes upon thread start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            // Connect to the database
            Database database = new Database();
            database.connect();
            
            String reply = null;
            switch (type) {
			case JOIN:
				JoinEvent<PircBotX> je = (JoinEvent<PircBotX>) event;
				reply = Introduce.instance_of().join(database.getConnection(), je.getChannel(), je.getUser());
				break;
			case CHATTER:
				MessageEvent<PircBotX> me = (MessageEvent<PircBotX>) event;
				reply = Introduce.instance_of().talk(database.getConnection(), me.getChannel(), me.getUser());
				break;
			case PM:
				// TODO: check the command
				PrivateMessageEvent<PircBotX> pe = (PrivateMessageEvent<PircBotX>) event;
				reply = Introduce.instance_of().(database.getConnection(), me.getChannel(), me.getUser());
				break;
			default:
				break;
			}

            // Disconnect from the database
            database.disconnect();
            
            if (reply != null)
            	event.respond(reply);

        } catch (Exception ex) {
            Configuration.getLogger().write(Level.WARNING, ex.getStackTrace().toString());
            return;
        }
    }
}
