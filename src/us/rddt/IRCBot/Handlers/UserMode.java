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
import org.pircbotx.hooks.events.MessageEvent;

import us.rddt.IRCBot.IRCUtils;
import us.rddt.IRCBot.IRCUtils.UserModes;

public class UserMode implements Runnable {
	// Variables
	private MessageEvent<PircBotX> event;
	private UserModes mode;

	// Method that executes upon start of thread
	public void run() {
		switch(mode) {
		case KICK:
			kickUser(false);
			break;
		case BAN:
			kickUser(true);
			break;
		default:
			break;
		}
	}

	// Class constructor
	public UserMode(MessageEvent<PircBotX> event, UserModes mode) {
		this.event = event;
		this.mode = mode;
	}

	private void kickUser(boolean isBan) {
		// Temporary variables
		String kickUser = event.getMessage().split(" ")[1];
		String kickReason = getReason();
		// Ensure that the kick command is allowable (user is an op and is kicking someone below their level)
		if(isAllowable(event.getChannel(), event.getUser(), event.getBot().getUser(kickUser))) {
			// Don't allow users to kick the bot
			if(!kickUser.equals(event.getBot())) {
				// Kick the offending user! (Reason optional)
				if(kickReason != "") {
					event.getBot().kick(event.getChannel(), event.getBot().getUser(kickUser), kickReason + " (" + event.getUser().getNick() + ")");
				} else {
					event.getBot().kick(event.getChannel(), event.getBot().getUser(kickUser), "Requested (" + event.getUser().getNick() + ")");
				}
				// Log the kick
				IRCUtils.Log(IRCUtils.LogLevels.INFORMATION, kickUser + " has been kicked from the channel by " + event.getUser().getNick() + ".");
				// If we're also to ban the user, and the op is not a half op, ban the user and log it as well
				if(isBan && !event.getUser().getChannelsHalfOpIn().contains(event.getChannel())) {
					event.getBot().ban(event.getChannel(), event.getBot().getUser(kickUser).getHostmask());
					IRCUtils.Log(IRCUtils.LogLevels.INFORMATION, kickUser + " (hostmask " + event.getBot().getUser(kickUser).getHostmask() + ") has been banned from the channel by " + event.getUser().getNick() + ".");
				}
			} else {
				event.getBot().kick(event.getChannel(), event.getUser(), "You are not allowed to kick the bot.");
			}
		}
	}

	// Returns the specified reason for kicking the user
	private String getReason() {
		String[] split = event.getMessage().split(" ");
		String reason = "";
		if(split.length > 1) {
			for(int i = 2; i < split.length; i++) {
				reason += split[i] + " ";
			}
		}
		return reason.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
	}

	// Ensures that the kick/ban operation is valid within IRC rules
	private boolean isAllowable(Channel channel, User op, User toKick) {
		// If the op is the channel owner, allow it
		if(op.getChannelsOwnerIn().contains(channel)) return true;
		// If the op is a superop AND the offending user is NOT an owner
		else if(op.getChannelsSuperOpIn().contains(channel) && !toKick.getChannelsOwnerIn().contains(toKick)) return true;
		// If the op is an op AND the offending user is NOT a superop OR owner
		else if(op.getChannelsOpIn().contains(channel) && !toKick.getChannelsSuperOpIn().contains(toKick) && !toKick.getChannelsOwnerIn().contains(toKick)) return true;
		// If the op is a halfop AND the offending user is NOT an op OR superop OR owner
		else if(op.getChannelsHalfOpIn().contains(channel) && !op.getChannelsOpIn().contains(channel) && !toKick.getChannelsSuperOpIn().contains(toKick) && !toKick.getChannelsOwnerIn().contains(toKick)) return true;
		// The operation is illegal!
		else return false;
	}
}
