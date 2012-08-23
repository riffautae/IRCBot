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

package us.rddt.IRCBot;

import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;

import us.rddt.IRCBot.Enums.TopicUpdates;
import us.rddt.IRCBot.Enums.UserModes;
import us.rddt.IRCBot.Handlers.Calculator;
import us.rddt.IRCBot.Handlers.Convert;
import us.rddt.IRCBot.Handlers.Define;
import us.rddt.IRCBot.Handlers.Fortune;
import us.rddt.IRCBot.Handlers.GameStatus;
import us.rddt.IRCBot.Handlers.SourceServerQuery;
import us.rddt.IRCBot.Handlers.Search;
import us.rddt.IRCBot.Handlers.Seen;
import us.rddt.IRCBot.Handlers.Shouts;
import us.rddt.IRCBot.Handlers.SteamUserQuery;
import us.rddt.IRCBot.Handlers.Topic;
import us.rddt.IRCBot.Handlers.UserMode;
import us.rddt.IRCBot.Handlers.Votekick;
import us.rddt.IRCBot.Implementations.URLGrabber;

/**
 * @author Ryan Morrison
 */
public class IRCBotHandlers extends ListenerAdapter<PircBotX> {
    /**
     * Checks incoming messages from users for potential bot commands
     * @param event the MessageEvent to parse
     * @return true if a command was parsed, false if no command was recognized
     */
    private boolean checkForCommands(MessageEvent<PircBotX> event) {
        /*
         * Most commands below spawn threads to prevent blocking.
         */
        if(event.getMessage().equals("!who last")) {
            if(!Configuration.getDisabledFunctions().contains("shout")) {
                new Thread(new Shouts(event, Shouts.ShoutEvents.LAST_COMMAND)).start();
                return true;
            }
        }
        if(event.getMessage().equals("!who list")) {
            if(!Configuration.getDisabledFunctions().contains("shout")) {
                new Thread(new Shouts(event, Shouts.ShoutEvents.LIST_COMMAND)).start();
                return true;
            }
        }
        if(event.getMessage().equals("!who top10")) {
            if(!Configuration.getDisabledFunctions().contains("shout")) {
                new Thread(new Shouts(event, Shouts.ShoutEvents.TOP10_COMMAND)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!who delete ")) {
            if(!Configuration.getDisabledFunctions().contains("shout")) {
                if(isUserOperator(event.getUser(), event.getChannel())) {
                    new Thread(new Shouts(event, Shouts.ShoutEvents.DELETE_COMMAND)).start();
                }
                return true;
            }
        }
        if(event.getMessage().startsWith("!who ")) {
            if(!Configuration.getDisabledFunctions().contains("shout")) {
                new Thread(new Shouts(event, Shouts.ShoutEvents.LOOKUP_COMMAND)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!calc ")) {
            if(!Configuration.getDisabledFunctions().contains("calc")) {
                new Thread(new Calculator(event)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!convert ")) {
            if(!Configuration.getDisabledFunctions().contains("convert")) {
                new Thread(new Convert(event)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!decide ")) {
            if(!Configuration.getDisabledFunctions().contains("fortune")) {
                new Thread(new Fortune(event)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!seen ")) {
            if(!Configuration.getDisabledFunctions().contains("seen")) {
                new Thread(new Seen(event)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!ud ")) {
            if(!Configuration.getDisabledFunctions().contains("urbandictionary")) {
                new Thread(new Define(event)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!g ")) {
            if(!Configuration.getDisabledFunctions().contains("google")) {
                new Thread(new Search(event)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!query ")) {
            if(!Configuration.getDisabledFunctions().contains("sourcequery")) {
                new Thread(new SourceServerQuery(event)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!steam ")) {
            if(!Configuration.getDisabledFunctions().contains("steamquery")) {
                new Thread(new SteamUserQuery(event)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!status ")) {
            if(!Configuration.getDisabledFunctions().contains("gamestatus")) {
                new Thread(new GameStatus(event)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!votekick ")) {
            if(!Configuration.getDisabledFunctions().contains("votekick")) {
                new Thread(new Votekick(event)).start();
            }
        }
        if(event.getMessage().startsWith("!appendtopic ")) {
            if(isUserOperator(event.getUser(), event.getChannel())) {
                new Thread(new Topic(event, TopicUpdates.ADD_TO_TOPIC)).start();
                return true;
            }
        }
        if(event.getMessage().startsWith("!removetopic ")) {
            if(isUserOperator(event.getUser(), event.getChannel())) {
                new Thread(new Topic(event, TopicUpdates.REMOVE_FROM_TOPIC)).start();
                return true;
            }
        }
        if(event.getMessage().equals("!leave")) {
            if(isUserAdmin(event.getUser())) {
                event.getBot().partChannel(event.getChannel());
                return true;
            }
        }

        /*
         * User mode change events
         */
        if(event.getMessage().startsWith("!kick ") || event.getMessage().substring(0, 3).equals(".k ")) {
            new Thread(new UserMode(event, UserModes.KICK)).start();
            return true;
        }
        if(event.getMessage().startsWith("!kickban ") || event.getMessage().substring(0, 4).equals(".kb ")) {
            new Thread(new UserMode(event, UserModes.BAN)).start();
            return true;
        }
        if(event.getMessage().startsWith("!owner ")) {
            new Thread(new UserMode(event, UserModes.OWNER)).start();
            return true;
        }
        if(event.getMessage().startsWith("!deowner ")) {
            new Thread(new UserMode(event, UserModes.DEOWNER)).start();
            return true;
        }
        if(event.getMessage().startsWith("!protect ")) {
            new Thread(new UserMode(event, UserModes.SUPEROP)).start();
            return true;
        }
        if(event.getMessage().startsWith("!deprotect ")) {
            new Thread(new UserMode(event, UserModes.DESUPEROP)).start();
            return true;
        }
        if(event.getMessage().startsWith("!op ")) {
            new Thread(new UserMode(event, UserModes.OP)).start();
            return true;
        }
        if(event.getMessage().startsWith("!deop ")) {
            new Thread(new UserMode(event, UserModes.DEOP)).start();
            return true;
        }
        if(event.getMessage().startsWith("!halfop ")) {
            new Thread(new UserMode(event, UserModes.HALFOP)).start();
            return true;
        }
        if(event.getMessage().startsWith("!dehalfop ")) {
            new Thread(new UserMode(event, UserModes.DEHALFOP)).start();
            return true;
        }
        if(event.getMessage().startsWith("!voice ")) {
            new Thread(new UserMode(event, UserModes.VOICE)).start();
            return true;
        }
        if(event.getMessage().startsWith("!devoice ")) {
            new Thread(new UserMode(event, UserModes.DEVOICE)).start();
            return true;
        }
        return false;
    }

    /**
     * Checks to see if a string is uppercase
     * @param s the string to check
     * @return true if the string is uppercase, false if it is not
     */
    private boolean isUpperCase(String s) {
        // Boolean value to ensure that an all numeric string does not trigger the shouting functions
        boolean includesLetter = false;
        // Loop through each character in the string individually
        for(int i = 0; i < s.length(); i++) {
            // If there's at least one letter then the string could qualify as being a 'shout'
            if(Character.isLetter(s.charAt(i))) includesLetter = true;
            // Any lower case letters immediately disqualifies the string, return immediately instead of continuing the loop
            if(Character.isLowerCase(s.charAt(i))) return false;
        }
        // If there's at least one letter in the string return true, otherwise disqualify it
        if(includesLetter) return true;
        else return false;
    }

    /**
     * Handler when a channel invite has been received
     * (non-Javadoc)
     * @see org.pircbotx.hooks.ListenerAdapter#onInvite(org.pircbotx.hooks.events.InviteEvent)
     * @param event the InviteEvent to parse
     */
    public void onInvite(InviteEvent<PircBotX> event) {
        if(event.getUser().equals(Configuration.getAdminNick())) event.getBot().joinChannel(event.getChannel());
        return;
    }
    
    /**
     * Handler when someone has joined the channel
     * (non-Javadoc)
     * @see org.pircbotx.hooks.ListenerAdapter#onJoin(org.pircbotx.hooks.events.JoinEvent)
     * @param event the JoinEvent to parse
     */
    public void onJoin(JoinEvent<PircBotX> event) {
        if(!Configuration.getChannelAnnouncement().equals("") && Arrays.asList(Configuration.getChannelsParticipating()).contains(event.getChannel().getName())) {
            event.getBot().sendMessage(event.getUser(), "ANNOUNCEMENT: " + Configuration.getChannelAnnouncement());
        }
    }

    /**
     * Handler when the bot has been kicked from the channel
     * (non-Javadoc)
     * @see org.pircbotx.hooks.ListenerAdapter#onKick(org.pircbotx.hooks.events.KickEvent)
     * @param event the KickEvent to parse
     */
    public void onKick(KickEvent<PircBotX> event) {
        // Nobody should be able to kick the bot from the channel, so rejoin immediately if we are kicked
        event.getBot().joinChannel(event.getChannel().getName());
    }

    /**
     * Handler when messages are received from the bot
     * (non-Javadoc)
     * @see org.pircbotx.hooks.ListenerAdapter#onMessage(org.pircbotx.hooks.events.MessageEvent)
     * @param event the MessageEvent to parse
     * @throws Exception
     */
    public void onMessage(MessageEvent<PircBotX> event) throws Exception {
        // If the message is in upper case and not from ourselves, spawn a new thread to handle the shout
        if(isUpperCase(event.getMessage()) && event.getMessage().replaceAll("^\\s+", "").replaceAll("\\s+$", "").length() > 5 && event.getUser() != event.getBot().getUserBot()) {
            new Thread(new Shouts(event, Shouts.ShoutEvents.RANDOM_SHOUT)).start();
            return;
        }
        if(event.getMessage().charAt(0) == '!' || event.getMessage().charAt(0) == '.') {
            if(checkForCommands(event)) return;
        }
        if(!Configuration.getDisabledFunctions().contains("url")) {
            // Use a regex pattern to match URLs out of user messages
            int urlCount = 0;
            Pattern urlPattern = Pattern.compile("\\bhttps?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
            Matcher urlMatcher = urlPattern.matcher(event.getMessage());
            while(urlMatcher.find()) {
                if(++urlCount > 2) break;
                new Thread(new URLGrabber(event, new URL(urlMatcher.group()))).start();
            }
        }
    }

    /**
     * Handler when a user has left the channel
     * (non-Javadoc)
     * @see org.pircbotx.hooks.ListenerAdapter#onPart(org.pircbotx.hooks.events.PartEvent)
     * @param event the PartEvent to parse
     */
    public void onPart(PartEvent<PircBotX> event) {
        if(!Configuration.getDisabledFunctions().contains("seen")) {
            new Thread(new Seen(event)).start();
        }
    }

    /**
     * Handler when a private message has been sent to the bot
     * (non-Javadoc)
     * @see org.pircbotx.hooks.ListenerAdapter#onPrivateMessage(org.pircbotx.hooks.events.PrivateMessageEvent)
     * @param event the PrivateMessageEvent to parse
     */
    public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) {
        if(isUserAdmin(event.getUser())) {
            if(event.getMessage().startsWith("announce ")) {
                sendAnnouncement(event.getBot(), false, event.getMessage());
                return;
            }
            if(event.getMessage().startsWith("notice ")) {
                sendAnnouncement(event.getBot(), true, event.getMessage());
                return;
            }
            if(event.getMessage().equals("disconnect")) {
                Configuration.getLogger().write(Level.INFO, "Disconnecting due to administrator request");
                event.getBot().quitServer("Disconnecting due to administrator request");
                System.exit(0);
            }
            if(event.getMessage().equals("reload")) {
                Configuration.getLogger().write(Level.INFO, "Reloading configuration due to administrator request...");
                sendGlobalMessage(event.getBot(), "Reloading configuration...");
                try {
                    Configuration.loadConfiguration();
                    Configuration.startScheduler(event.getBot());
                } catch (Exception ex) {
                    Configuration.getLogger().write(Level.WARNING, ex.getStackTrace().toString());
                    sendGlobalMessage(event.getBot(), "Failed to reload configuration: " + ex.getMessage());
                }
                Configuration.getLogger().write(Level.INFO, "Reload complete");
                sendGlobalMessage(event.getBot(), "Successfully reloaded configuration.");
                return;
            }
            if(event.getMessage().equals("restart")) {
                Configuration.getLogger().write(Level.INFO, "Restarting due to administrator request...");
                sendGlobalMessage(event.getBot(), "Restarting due to administrator request...");
                try {
                    IRCUtils.restartApplication();
                } catch (Exception ex) {
                    Configuration.getLogger().write(Level.SEVERE, ex.getStackTrace().toString());
                }
            }
        } else {
            // There's no reason for anyone to privately message the bot - remind them that they are messaging a bot!
            event.respond("Hi! I am IRCBot version " + Configuration.getApplicationVersion() + ". If you don't know already, I'm just a bot and can't respond to your questions/comments. :( You might want to talk to my administrator, " + Configuration.getAdminNick() + " instead!");
        }
    }

    /**
     * Handler when a user disconnects from the IRC server
     * (non-Javadoc)
     * @see org.pircbotx.hooks.ListenerAdapter#onQuit(org.pircbotx.hooks.events.QuitEvent)
     * @param event the QuitEvent to parse
     */
    public void onQuit(QuitEvent<PircBotX> event) {
        if(!Configuration.getDisabledFunctions().contains("seen")) {
            new Thread(new Seen(event)).start();
        }
    }

    /**
     * Checks to see if a user is a bot administrator
     * @param user the user to check
     * @return true if the user is a bot administrator, false if they are not
     */
    private boolean isUserAdmin(User user) {
        if(user.getNick().equals(Configuration.getAdminNick()) && user.getHostmask().equals(Configuration.getAdminHostmask())) return true;
        else return false;
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
     * Sends a message to each channel the bot is currently in
     * @param bot the IRC bot
     * @param message the message to send
     */
    private void sendGlobalMessage(PircBotX bot, String message) {
        for(Channel c : bot.getChannels()) {
            bot.sendMessage(c, message);
        }
    }
    
    /**
     * Sends a targeted message or notice to a specific channel
     * @param bot the IRC bot
     * @param isNotice true if the announcement should be a CTCP notice, false if it should be a raw message
     * @param rawLine the raw line to be parsed
     */
    private void sendAnnouncement(PircBotX bot, boolean isNotice, String rawLine) {
        String[] splitLine = rawLine.split(" ");
        Channel channelToSend = bot.getChannel(splitLine[1]);
        if(bot.getChannels().contains(channelToSend)) {
            StringBuilder builtString = new StringBuilder();
            for(int i = 2; i < splitLine.length; i++) {
                builtString.append(splitLine[i] + " ");
            }
            if(isNotice) bot.sendNotice(channelToSend, builtString.toString());
            else bot.sendMessage(channelToSend, builtString.toString());
        }
    }
}
