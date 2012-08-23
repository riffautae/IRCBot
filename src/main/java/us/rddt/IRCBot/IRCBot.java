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

import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.exception.NickAlreadyInUseException;
import org.pircbotx.hooks.ListenerAdapter;

/**
 * @author Ryan Morrison
 */
public class IRCBot extends ListenerAdapter<PircBotX> {
    /*
     * Class variables
     */
    private static PircBotX bot;

    /**
     * The main entry point of the application
     * @param args arguments passed through the command line
     */
    public static void main(String[] args) throws Exception {
        try {
            Configuration.loadConfiguration();
        } catch(Exception ex) {
            Configuration.getLogger().write(Level.SEVERE, ex.getStackTrace().toString());
            System.exit(-1);
        }
        Configuration.getLogger().write(Level.INFO, "Initializing bot (IRCBot version " + Configuration.getApplicationVersion() + ")");
        
        // Create a new instance of the IRC bot
        bot = new PircBotX();
        // Add new listeners for the actions we want the bot to handle
        bot.getListenerManager().addListener(new IRCBotHandlers());
        // Set the bot's nick
        bot.setName(Configuration.getNick());
        // Set the bot's user
        bot.setLogin(Configuration.getUser());
        // Automatically split messages longer than IRC's size limit
        bot.setAutoSplitMessage(true);
        // Connect to the IRC server
        connect(bot);
        // Create the scheduler for watching subreddits
        Configuration.startScheduler(bot);
        // Add a shutdown handler to attempt to properly disconnect from the server upon shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if(bot.isConnected()) bot.quitServer("Received SIGINT from command line");
            }
        }));
    }

    /**
     * Connects to the IRC server
     * @param bot the IRC bot
     */
    private static void connect(PircBotX bot) {
        // Attempt to connect to the server and join the required channel(s)
        Configuration.getLogger().write(Level.INFO, "Connecting to " + Configuration.getServer() + " and joining channel(s).");
        try {
            if(Configuration.isSSL() && Configuration.isSSLVerified()) {
                bot.connect(Configuration.getServer(), Configuration.getPort(), Configuration.getPassword(), new UtilSSLSocketFactory());
            } else if (Configuration.isSSL() && !Configuration.isSSLVerified()) {
                bot.connect(Configuration.getServer(), Configuration.getPort(), Configuration.getPassword(), new UtilSSLSocketFactory().trustAllCertificates());
            } else {
                bot.connect(Configuration.getServer(), Configuration.getPort(), Configuration.getPassword());
            }
        } catch (NickAlreadyInUseException ex) {
            Configuration.getLogger().write(Level.WARNING, ex.getMessage());
            bot.setName(bot.getNick() + "_");
        } catch (Exception ex) {
            Configuration.getLogger().write(Level.SEVERE, ex.getMessage());
            bot.disconnect();
            System.exit(-1);
        }
        bot.sendRawLine("MODE " + bot.getNick() + " +B");
        joinChannels(Configuration.getChannels(), bot);
    }

    /**
     * Joins channels as defined in the bot's configuration
     * @param channels the channels to join
     * @param bot the IRC bot
     */
    private static void joinChannels(String[] channels, PircBotX bot) {
        for (int i = 0; i < channels.length; i++) {
            bot.joinChannel(channels[i]);
        }
    }
}