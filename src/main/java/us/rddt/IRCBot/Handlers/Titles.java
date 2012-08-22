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

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;

import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import java.sql.SQLException;

import us.rddt.IRCBot.Configuration;
import us.rddt.IRCBot.Database;
import us.rddt.IRCBot.IRCBot;
import us.rddt.IRCBot.Enums.CommandErrors;
import us.rddt.IRCBot.Enums.TitlesType;
import us.rddt.IRCBot.Implementations.TitleDB;

/**
 * Helper to parse message lines
 * @author Milton Thomas
 *
 */
class TitleCmdParse {
	//  											cmd   chan    user     params
	static final Pattern PAT_PM = Pattern.compile("(\\w+) (\\w+)(? (#\\w+) (.*))");
	//													cmd		user	params
	static final Pattern PAT_CHAT = Pattern.compile("\\!(\\w+)(? (\\w+) (.*))");
	
	public Boolean isPm;
	public String command;
	public String victim;
	public String channel;
	public String params;
	public Integer id;
	
	/**
	 * Parse an incoming command into something more useful
	 * @param isPm was this from a pm or a normal chat
	 * @param victim who is the victim
	 * @param channel where will this apply
	 * @param title what text are we using
	 * @param id alt, what is the numeric id of the title
	 */
	public TitleCmdParse(boolean isPm, String command, String victim, String channel, String params, Integer id) {
		this.isPm = isPm;
		this.command = command;
		this.victim = victim;
		this.channel = channel;
		this.params = params;
		this.id = id;
	}
		
	public static TitleCmdParse parsePm(String params) { 
		Matcher mat = PAT_PM.matcher(params);
		if (mat.groupCount() == 3) {
			try {
				return new TitleCmdParse(true, mat.group(1), mat.group(2), mat.group(3), null, Integer.parseInt(mat.group(3)));
			} catch (NumberFormatException e) {
				return new TitleCmdParse(true, mat.group(1), mat.group(2), mat.group(3), mat.group(4), null);
			} 
		} else {
			return null;
		}
	}
	
	public static TitleCmdParse parseChat(String chan, String params) { 
		Matcher mat = PAT_CHAT.matcher(params);
		if (mat.groupCount() == 3) {
			try {
				return new TitleCmdParse(true, mat.group(1), mat.group(2), chan, null, Integer.parseInt(mat.group(2)));
			} catch (NumberFormatException e) {
				return new TitleCmdParse(true, mat.group(1), mat.group(2), chan, mat.group(3), null);
			} 
			
		} else {
			return null;
		}
	}
}

/**
 * Executor class for title commands
 * @author Milton Thomas
 */
public class Titles implements Runnable {
	private Event<PircBotX> event;
	private TitlesType type;
	private Database database;
	
	public Titles(Event<PircBotX> event, TitlesType type) {
		this.event = event;
	}

	/**
     * Method that executes upon thread start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
        	database = new Database();
            database.connect();
            switch (type) {
			case JOIN:
				handleJoin();
				break;
			case CHATTER:
				handleChatt();
				break;
			case PM:
				handlePm();
				break;
            }
            database.disconnect();
        } catch (Exception ex) {
            Configuration.getLogger().write(Level.WARNING, ex.getStackTrace().toString());
        }
    }
    
    protected void handleJoin() throws SQLException {
		JoinEvent<PircBotX> je = (JoinEvent<PircBotX>) event;
		String announce = TitleDB.instanceOf().join(database.getConnection(), je.getChannel(), je.getUser());
		if (announce != null)
	    	je.respond(announce);
    }
    
    protected void handleChatt() throws SQLException {
		MessageEvent<PircBotX> me = (MessageEvent<PircBotX>) event;
		String message = me.getMessage();
		if( message.startsWith("!title ") ) {
			// commands
			TitleCmdParse icp = TitleCmdParse.parseChat(me.getChannel().getName(), me.getMessage());
			if (icp.command == "add") {
				if(icp.victim == me.getUser().getNick()) {
					event.respond(CommandErrors.NOT_ON_SELF.response);
					return;
				}
				if(!isUserVoice(me.getUser(), me.getChannel())) {
					event.respond(CommandErrors.WRONG_PERMISSIONS.response);
					return;
				}
				int ret = TitleDB.instanceOf().addTitle(database.getConnection(), 
						me.getUser().getNick(), icp.channel, icp.victim, icp.params);
				if( ret == 1 ) {
					IRCBot.getBot().sendMessage(me.getUser(), "Added title for " + icp.victim);
				}
			} else if (icp.command == "rem") {
				if(icp.victim == me.getUser().getNick()) {
					event.respond(CommandErrors.NOT_ON_SELF.response);
					return;
				}
				if(!isUserVoice(me.getUser(), me.getChannel())) {
					event.respond(CommandErrors.WRONG_PERMISSIONS.response);
					return;
				}
				int ret;
				if(icp.id != null) {
					ret = TitleDB.instanceOf().remTitleById(database.getConnection(), 
							icp.channel, icp.victim, icp.id);
				} else {
					ret = TitleDB.instanceOf().remTitleByString(database.getConnection(), 
							icp.channel, icp.victim, icp.params);
				}
				if( ret > 0 ) {
					IRCBot.getBot().sendMessage(me.getUser(), "Removed " + ret + " title(s) for " + icp.victim + 
							" from " + icp.channel);
				}
			} else if (icp.command == "stats") {
				String stats = TitleDB.instanceOf().getStats(
						database.getConnection(), icp.channel);
				
				if( stats != null ) {
					event.respond(stats);
				}
			} else if (icp.command == "top") {
				String top = TitleDB.instanceOf().getTop(
						database.getConnection(), icp.channel);
				
				if( top != null ) {
					event.respond(top);
				}
			}
		} else {
			// makes fun of users some times
			String announce = TitleDB.instanceOf().talk(database.getConnection(), 
					me.getChannel(), me.getUser());
		
			if (announce != null)
				event.respond(announce);
		}
    }

    protected void handlePm() throws SQLException, InterruptedException {
    	PrivateMessageEvent<PircBotX> me = (PrivateMessageEvent<PircBotX>) event;
		TitleCmdParse icp = TitleCmdParse.parsePm(me.getMessage());
		if (icp.command == "add") {
			if(icp.victim == me.getUser().getNick()) {
				event.respond(CommandErrors.NOT_ON_SELF.response);
				return;
			}
			if( !isUserVoice(me.getUser(), IRCBot.getBot().getChannel(icp.channel)) ) {
				event.respond(CommandErrors.WRONG_PERMISSIONS.response);
				return;
			}
			int ret = TitleDB.instanceOf().addTitle(database.getConnection(), 
					me.getUser().getNick(), icp.channel, icp.victim, icp.params);
			if( ret == 1 ) {
				IRCBot.getBot().sendMessage(me.getUser(), "Added title for " + icp.victim);
			}
		} else if (icp.command == "rem") {
			if( icp.victim == me.getUser().getNick() ) {
				event.respond(CommandErrors.NOT_ON_SELF.response);
				return;
			}
			if( !isUserVoice(me.getUser(), IRCBot.getBot().getChannel(icp.channel)) ) {
				event.respond(CommandErrors.WRONG_PERMISSIONS.response);
				return;
			}
			int ret;
			if(icp.id != null) {
				ret = TitleDB.instanceOf().remTitleById(database.getConnection(), 
						icp.channel, icp.victim, icp.id);
			} else {
				ret = TitleDB.instanceOf().remTitleByString(database.getConnection(), 
						icp.channel, icp.victim, icp.params);
			}
			if( ret > 0 ) {
				IRCBot.getBot().sendMessage(me.getUser(), "Removed " + ret + " title(s) for " + icp.victim + 
						" from " + icp.channel);
			}
		} else if (icp.command == "stats") {
			String stats = TitleDB.instanceOf().getStats(
					database.getConnection(), icp.channel);
			
			if( stats != null ) {
				event.respond(stats);
			}
		} else if (icp.command == "top") {
			String top = TitleDB.instanceOf().getTop(
					database.getConnection(), icp.channel);
			
			if( top != null ) {
				event.respond(top);
			}
		} else if (icp.command == "list") {
			List<String> list = TitleDB.instanceOf().listTitles
					(database.getConnection(), icp.channel, icp.victim, icp.id);
			
			if( list != null ) {
				for( String s : list) {
					this.wait(500); // wait 1/2 second between posting multiple things
					me.respond(s);
				}
			}
		} else if (icp.command == "last") {
			List<String> list = TitleDB.instanceOf().lastTitles
					(database.getConnection(), icp.channel);
			
			if( list != null ) {
				for( String s : list) {
					this.wait(500); // wait 1/2 second between posting multiple things
					me.respond(s);
				}
			}
		}
    }
    
    /**
     * Checks to see if a user is a channel operator or higher
     * @param user the user to check
     * @param channel the channel to check against
     * @return true if the user is a channel operator or higher, false if they are not
     */
    private boolean isUserOperator(User user, Channel channel) {
        if(channel.isOp(user) || channel.isSuperOp(user) || channel.isOwner(user)) return true;
        else return false;
    }
    
    /**
     * Checks to see if a user is voice or higher
     * @param user the user to check
     * @param channel the channel to check against
     * @return true if the user is a channel operator or higher, false if they are not
     */
    private boolean isUserVoice(User user, Channel channel) {
        if(isUserOperator(user, channel) || channel.isHalfOp(user) || channel.hasVoice(user)) return true;
        else return false;
    }
}
