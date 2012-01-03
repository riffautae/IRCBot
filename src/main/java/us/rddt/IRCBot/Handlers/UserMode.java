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

import us.rddt.IRCBot.Enums.UserModes;

/**
 * @author Ryan Morrison
 */
public class UserMode implements Runnable {
	/*
	 * Class variables.
	 */
	private MessageEvent<PircBotX> event;
	private UserModes mode;

	/**
	 * Class constructor
	 * @param event the MessageEvent that triggered this class
	 * @param mode the mode to enforce on the user
	 */
	public UserMode(MessageEvent<PircBotX> event, UserModes mode) {
		this.event = event;
		this.mode = mode;
	}

	/**
	 * Changes the user mode if the request is in accordance with IRC rules
	 * @param mode the user mode to apply
	 */
	private void changeMode(UserModes mode) {
		String modeUser = event.getMessage().split(" ")[1];
		if(isAllowable(event.getChannel(), event.getUser(), event.getBot().getUser(modeUser))) {
			switch(mode) {
			case OWNER:
				event.getBot().owner(event.getChannel(), event.getBot().getUser(modeUser));
				break;
			case DEOWNER:
				event.getBot().deOwner(event.getChannel(), event.getBot().getUser(modeUser));
				break;
			case SUPEROP:
				event.getBot().superOp(event.getChannel(), event.getBot().getUser(modeUser));
				break;
			case DESUPEROP:
				event.getBot().deSuperOp(event.getChannel(), event.getBot().getUser(modeUser));
				break;
			case OP:
				event.getBot().op(event.getChannel(), event.getBot().getUser(modeUser));
				break;
			case DEOP:
				event.getBot().deOp(event.getChannel(), event.getBot().getUser(modeUser));
				break;
			case HALFOP:
				event.getBot().halfOp(event.getChannel(), event.getBot().getUser(modeUser));
				break;
			case DEHALFOP:
				event.getBot().deHalfOp(event.getChannel(), event.getBot().getUser(modeUser));
				break;
			case VOICE:
				event.getBot().voice(event.getChannel(), event.getBot().getUser(modeUser));
				break;
			case DEVOICE:
				event.getBot().deVoice(event.getChannel(), event.getBot().getUser(modeUser));
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Returns the provided reason for the kick/ban
	 * @return the provided reason for the kick/ban
	 */
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

	/**
	 * Ensure the kick/ban operation is in accordance to IRC rules
	 * @param channel the channel the operation is being performed on
	 * @param op the op requesting the mode change
	 * @param toChange the user to receive the mode change
	 */
	private boolean isAllowable(Channel channel, User op, User toChange) {
		// If the op is the channel owner, allow it
		if(op.getChannelsOwnerIn().contains(channel)) return true;
		// If the op is a superop AND the offending user is NOT another superop or owner
		else if(op.getChannelsSuperOpIn().contains(channel) && !toChange.getChannelsSuperOpIn().contains(channel) && !toChange.getChannelsOwnerIn().contains(channel)) return true;
		// If the op is an op AND the offending user is NOT an op OR superop OR owner
		else if(op.getChannelsOpIn().contains(channel) && !toChange.getChannelsOpIn().contains(channel) && !toChange.getChannelsSuperOpIn().contains(channel) && !toChange.getChannelsOwnerIn().contains(channel)) return true;
		// If the op is a halfop AND the offending user is NOT a halfop OR op OR superop OR owner
		else if(op.getChannelsHalfOpIn().contains(channel) && !toChange.getChannelsHalfOpIn().contains(channel) && !toChange.getChannelsOpIn().contains(channel) && !toChange.getChannelsSuperOpIn().contains(channel) && !toChange.getChannelsOwnerIn().contains(channel)) return true;
		// The operation is illegal!
		else return false;
	}
	
	/**
	 * Kicks (and bans) a user from the channel
	 * @param isBan true if the user should be banned as well, false if kicking only
	 */
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
				// If we're also to ban the user, and the op is not a half op, ban the user and log it as well
				if(isBan && !event.getUser().getChannelsHalfOpIn().contains(event.getChannel())) {
					event.getBot().ban(event.getChannel(), event.getBot().getUser(kickUser).getHostmask());
				}
			} else {
				event.getBot().kick(event.getChannel(), event.getUser(), "You are not allowed to kick the bot.");
			}
		}
	}

	/**
	 * Method that executes upon thread start
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// Execute the appropriate actions based on the mode to change.
		switch(mode) {
		case KICK:
			kickUser(false);
			break;
		case BAN:
			kickUser(true);
			break;
		default:
			changeMode(mode);
			break;
		}
	}
}
