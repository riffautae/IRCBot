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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import java.sql.SQLException;

import us.rddt.IRCBot.Configuration;
import us.rddt.IRCBot.Database;
import us.rddt.IRCBot.IRCBot;
import us.rddt.IRCBot.IRCUtils;
import us.rddt.IRCBot.SillyConfiguration;
import us.rddt.IRCBot.Enums.CommandErrors;
import us.rddt.IRCBot.Enums.TitlesType;
import us.rddt.IRCBot.Implementations.RecentAnnouncement;
import us.rddt.IRCBot.Implementations.RecentData;
import us.rddt.IRCBot.Implementations.TitleDB;

/**
 * Helper to parse message lines
 * @author Milton Thomas
 */
class TitleCmdParse {
	//  											       cmd   chan    user     params
	static final Pattern PAT_PM = Pattern.compile("title (\\w+) (#\\w+)(?: (\\w+) (.*))?");
	//													      cmd	  user     params
	static final Pattern PAT_CHAT = Pattern.compile("!title (\\w+)(?: (\\w+) (.*))?");
	
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
	    if(mat.matches()) {
	        switch (mat.groupCount()) {
	        case 4:
	            try {
	                return new TitleCmdParse(true, mat.group(1), mat.group(3), mat.group(2), null, Integer.parseInt(mat.group(3)));
	            } catch (NumberFormatException e) {
	                return new TitleCmdParse(true, mat.group(1), mat.group(3), mat.group(2), mat.group(4), null);
	            } 
	        case 2:
	            return new TitleCmdParse(true, mat.group(1), null, mat.group(2), null, null);
	        default:
	            return null;
	        }
	    } else {
	        return null;
	    }
	}

	public static TitleCmdParse parseChat(String chan, String params) {
	    Matcher mat = PAT_CHAT.matcher(params);
	    if(mat.matches()) {
	        switch (mat.groupCount()) {
	        case 3:
	            try {
	                return new TitleCmdParse(true, mat.group(1), mat.group(2), chan, null, Integer.parseInt(mat.group(3)));
	            } catch (NumberFormatException e) {
	                return new TitleCmdParse(true, mat.group(1), mat.group(2), chan, mat.group(3), null);
	            }
	        case 1:
	            return new TitleCmdParse(true, mat.group(1), null, chan, null, null);
	        default:
	            return null;
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
	
	// A recentData for each channel
	private Map<String, RecentData> recentTitles = new ConcurrentHashMap<String, RecentData>();

	public Titles(Event<PircBotX> event, TitlesType type) {
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
        	database = new Database();
            database.connect();
            switch (type) {
			case JOIN:
				handleJoin();
				break;
			case PART:
				handlePart();
				break;
			case CHATTER:
				handleChat();
				break;
			case PM:
				handlePm();
				break;
            }
            database.disconnect();
        } catch (Exception ex) {
            Configuration.getLogger().write(Level.WARNING, IRCUtils.getStackTraceString(ex));
        }
    }
    
    protected void handleJoin() throws SQLException {
		JoinEvent<PircBotX> je = (JoinEvent<PircBotX>) event;
		
		synchronized (recentTitles) {
			RecentData rd = getRecent(je.getChannel().getName());
			
			if (!rd.checkAnnounce(je.getUser().getNick()))
				return;
		}
		
		String announce = TitleDB.instanceOf().title(database.getConnection(), je.getChannel(), je.getUser());
		String alias = IRCUtils.choose(SillyConfiguration.getJoinWrapper());
		
		if (announce != null)
	    	je.respond(formatSilly(alias, je.getUser().getNick(), announce, je.getChannel()));
	}
	
	protected void handlePart() throws SQLException {
		PartEvent<PircBotX> pe = (PartEvent<PircBotX>) event;
		
		synchronized (recentTitles) {
			RecentData rd = getRecent(pe.getChannel().getName());
			
			if (!rd.checkAnnounce(pe.getUser().getNick()))
				return;
		}
		
		String announce = TitleDB.instanceOf().title(database.getConnection(), pe.getChannel(), pe.getUser());
		String alias = IRCUtils.choose(SillyConfiguration.getPartWrapper());
		
		if (announce != null)
	    	pe.respond(formatSilly(alias, pe.getUser().getNick(), announce, pe.getChannel()));
    }

	/**
	 * Handle chant commands and general chatter
	 * commands: !title add|rem|stats|top
	 * @throws SQLException
	 */
    protected void handleChat() throws SQLException {
		MessageEvent<PircBotX> me = (MessageEvent<PircBotX>) event;
		String message = me.getMessage();
		if( message.startsWith("!title ") ) {
			// commands
			TitleCmdParse icp = TitleCmdParse.parseChat(me.getChannel().getName(), me.getMessage());
			if (icp.command.equals("add")) {
				if(icp.victim == me.getUser().getNick()) {
					me.respond(CommandErrors.NOT_ON_SELF.response);
					return;
				}
				if(!isUserVoice(me.getUser(), me.getChannel())) {
					me.respond(CommandErrors.WRONG_PERMISSIONS.response);
					return;
				}
				int ret = TitleDB.instanceOf().addTitle(database.getConnection(), 
						me.getUser().getNick(), icp.channel, icp.victim, icp.params);
				if( ret == 1 ) {
					IRCBot.getBot().sendMessage(me.getUser(), "Added title for " + icp.victim);
				}
			} else if (icp.command.equals("rem")) {
				if(icp.victim == me.getUser().getNick()) {
					me.respond(CommandErrors.NOT_ON_SELF.response);
					return;
				}
				if(!isUserOperator(me.getUser(), me.getChannel())) {
					me.respond(CommandErrors.WRONG_PERMISSIONS.response);
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
			} else if (icp.command.equals("stats")) {
				String[] stats = TitleDB.instanceOf().getStats(
						database.getConnection(), icp.channel);
				
				if( stats != null ) {
					me.respond(formatStats(stats));
				}
			} else if (icp.command.equals("top")) {
				List<String[]> topSub = TitleDB.instanceOf().getTopSubmitters(
						database.getConnection(), icp.channel);
				List<String[]> topVic = TitleDB.instanceOf().getTopVictims(
                        database.getConnection(), icp.channel);
				if( topSub != null && topVic != null ) {
					me.getBot().sendMessage(me.getChannel(), formatTop(topSub, topVic));
				}
			} else if (icp.command.equals("last")) {
				String[] last = lastTitle(icp.channel);
				
				if( last != null ) {
					me.respond(formatTitle(last));
				}
			}
		} else {
			synchronized (recentTitles) {	
				RecentData rd = getRecent(me.getChannel().getName());
			
				// one in twenty chance that we make fun of them
				// Also must be in monologue and not too busy in the chat
				if (!(rd.checkAnnounce(me.getUser().getNick()) && rd.inMonologue(me.getUser().getNick()) && (new Random()).nextInt(20)==1 ))
				    return;
			}
				
			// makes fun of users some times
			String announce = TitleDB.instanceOf().title(database.getConnection(), 
					me.getChannel(), me.getUser());
			
			String alias = IRCUtils.choose(SillyConfiguration.getChatterWrapper());
			
			if (announce != null)
		    	me.getBot().sendMessage(me.getChannel(), formatSilly(alias, me.getUser().getNick(), announce, me.getChannel()));
		}
    }

    protected void handlePm() throws SQLException, InterruptedException {
    	PrivateMessageEvent<PircBotX> me = (PrivateMessageEvent<PircBotX>) event;
		TitleCmdParse icp = TitleCmdParse.parsePm(me.getMessage());
		if (icp.command.equals("add")) {
			if( !isUserVoice(me.getUser(), IRCBot.getBot().getChannel(icp.channel))) return;
			if(icp.victim.equals(me.getUser().getNick())) {
				me.respond(CommandErrors.NOT_ON_SELF.response);
				return;
			}
			if( !isUserVoice(me.getUser(), IRCBot.getBot().getChannel(icp.channel)) ) {
				me.respond(CommandErrors.WRONG_PERMISSIONS.response);
				return;
			}
			int ret = TitleDB.instanceOf().addTitle(database.getConnection(), 
					me.getUser().getNick(), icp.channel, icp.victim, icp.params);
			if( ret == 1 ) {
				IRCBot.getBot().sendMessage(me.getUser(), "Added title for " + icp.victim);
			}
		} else if (icp.command.equals("rem")) {
			if( icp.victim == me.getUser().getNick() ) {
				me.respond(CommandErrors.NOT_ON_SELF.response);
				return;
			}
			if( !isUserOperator(me.getUser(), IRCBot.getBot().getChannel(icp.channel)) ) {
				me.respond(CommandErrors.WRONG_PERMISSIONS.response);
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
		} else if (icp.command.equals("stats")) {
			String[] stats = TitleDB.instanceOf().getStats(
					database.getConnection(), icp.channel);
			
			if( stats != null ) {
				me.respond(formatStats(stats));
			}
		} else if (icp.command.equals("top")) {
		    List<String[]> topSub = TitleDB.instanceOf().getTopSubmitters(
                    database.getConnection(), icp.channel);
            List<String[]> topVic = TitleDB.instanceOf().getTopVictims(
                    database.getConnection(), icp.channel);
            if( topSub != null && topVic != null ) {
                me.respond(formatTop(topSub, topVic));
            }
		} else if (icp.command.equals("list")) {
			List<String[]> list = TitleDB.instanceOf().listTitles
					(database.getConnection(), icp.channel, icp.victim, icp.id);
			
			if( list != null ) {
				for( String s : formatTitles(list)) {
					this.wait(500); // wait 1/2 second between posting multiple things
					me.respond(s);
				}
			}
		} else if (icp.command.equals("last")) {
			List<String[]> list = lastTitles(icp.channel);
			if( list != null ) {
				for( String s : formatTitles(list)) {
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
    
    // formating helpers =============
    
    private static Pattern PAT_NAME = Pattern.compile("%n");
    private static Pattern PAT_TITLE = Pattern.compile("%t");
    private static Pattern PAT_RANDOM = Pattern.compile("%r");
    
    private String formatSilly(String format, String user, String title, Channel channel) {
    	String ret = format;
    	Matcher r = PAT_RANDOM.matcher(ret);
    	if( r.find() ) {
    		String rNick = IRCUtils.choose( channel.getUsers() ).getNick();
    		ret = r.replaceAll(rNick);
    	}
    	Matcher n = PAT_NAME.matcher(ret);
    	if( n.find() ) {
    		ret = n.replaceAll(user);
    	}
    	Matcher t = PAT_TITLE.matcher(ret);
    	if( t.find() ) {
    		ret = t.replaceAll(title);
    	}
    	return ret;
    }
    
    /**
     * Formats a stats string for printing
     * @param stats Array of [title count, jerk name, jerk count, victim name, victim count]
     * @return Formatted string ready for printing
     */
    private String formatStats(String[] stats) {
    	String[] alias = IRCUtils.choose( SillyConfiguration.getSubVicAliases() );
    	return "I have " + stats[0] + " titles. The " + alias[0] + " is " + stats[1] + " (" + stats[2] + "). The " +
    			alias[1] + " is " + stats[3] + " (" + stats[4] + ").";
    }
    
    /**
     * Formats a list of top lines for printing
     * 
     * @param stats 5 jerks + 5 victims in an array of [name, title count]
     * @return Formatted strings ready for printing
     */
    private String formatTop(List<String[]> submitters, List<String[]> victims) {
    	String[] alias = IRCUtils.choose( SillyConfiguration.getSubVicAliases() );
    	
    	List<String> statSubmitters = new LinkedList<String>();
    	for(String[] stat : submitters) {
    		statSubmitters.add(stat[0] + " (" + stat[1] + "]");
    	}
    	List<String> statVictims = new LinkedList<String>();
        for(String[] stat : victims) {
            statVictims.add(stat[0] + " (" + stat[1] + "]");
        }
    	
    	String subList = IRCUtils.join(statSubmitters.subList(0, statSubmitters.size()), ", ");
    	String vicList = IRCUtils.join(statVictims.subList(0, statVictims.size()), ", ");
    	
    	return "The " + alias[0] + "s: " + subList + " - the " + alias[1] + "s: " + vicList;
    }
    
    /**
     * Format a list of titles
     * Form:
     * [id, title text]
     * OR
     * [id, user, title text]
     * @param titles
     * @return
     */
    private List<String> formatTitles(List<String[]> titles) {
    	List<String> ret = new LinkedList<String>();
    	for(String[] t : titles) {
    		String r = formatTitle(t);
    		if (r != null)
    			ret.add(r);
    	}
    	return ret;
    }
    
    /**
     * Format a title
     * Form:
     * [id, title text]
     * OR
     * [id, user, title text]
     * @param title
     * @return
     */
    private String formatTitle(String[] title) {
		if (title.length == 2)
			return title[0] + ") " + title[1] + ".";
		else if (title.length == 3)
			return title[0] + ") " + title[1] + ": " + title[2] + ".";
		else
			return null;
    }
    
    // recent data helpers =============
    
    /**
	 * Get a brief history of titles used in the chat
	 * @param conn db connection
	 * @param channel irc channel
	 * @return List of [id, user, title]
	 */
	private List<String[]> lastTitles(String channel) {
		synchronized (recentTitles) {
			RecentData rd = getRecent(channel);
			List<String[]> resp = new LinkedList<String[]>();
			for( RecentAnnouncement ra : rd.listHistory() ) {
				String[] line = {
						String.valueOf(ra.getTitleId()), 
						ra.getUser(), 
						ra.getTitle()
						};
				resp.add(line);
			}
			return resp;
		}
	}
	
	/**
	 * Get a brief history of titles used in the chat
	 * @param conn db connection
	 * @param channel irc channel
	 * @return List of [id, user, title]
	 */
	private String[] lastTitle(String channel) {
		synchronized (recentTitles) { //TODO: this everywhere
			RecentData rd = getRecent(channel);
			RecentAnnouncement ra = rd.listHistory().peek();
			String[] resp = {
					String.valueOf(ra.getTitleId()), 
					ra.getUser(), 
					ra.getTitle()
					};
			return resp;
		}
	}
	
	/**
	 * Get the recent data for a specific channel
	 * Will create it if needed
	 * Need to obtain lock on recentTitles to use
	 * @param chanName
	 * @return
	 */
	private RecentData getRecent(String chanName) {
		RecentData recent;
		if (recentTitles.containsKey(chanName)) {
			recent = recentTitles.get(chanName);
		} else {
			recent = new RecentData();
			recentTitles.put(chanName, recent);
		}
		return recent;
	}
	
}
