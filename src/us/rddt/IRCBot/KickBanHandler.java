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

import org.pircbotx.hooks.events.MessageEvent;

public class KickBanHandler implements Runnable {
	private MessageEvent event;
	private boolean isBan;

	public void run() {
		String kickUser = event.getMessage().split(" ")[1];
		String kickReason = getReason();
		if((event.getUser().getChannelsOpIn().contains(event.getChannel()) && !event.getBot().getUser(kickUser).getChannelsSuperOpIn().contains(event.getChannel()) && !event.getBot().getUser(kickUser).getChannelsOwnerIn().contains(event.getChannel()))
				|| (event.getUser().getChannelsSuperOpIn().contains(event.getChannel()) && !event.getBot().getUser(kickUser).getChannelsOwnerIn().contains(event.getChannel()))
				|| (event.getUser().getChannelsOwnerIn().contains(event.getChannel()))) {
			if(kickReason != "") {
				event.getBot().kick(event.getChannel(), event.getBot().getUser(kickUser), kickReason + " (" + event.getUser().getNick() + ")");
			} else {
				event.getBot().kick(event.getChannel(), event.getBot().getUser(kickUser), "Requested (" + event.getUser().getNick() + ")");
			}
			IRCUtils.Log(IRCUtils.LOG_INFORMATION, kickUser + " has been kicked from the channel by " + event.getUser().getNick() + ".");
			if(isBan) {
				event.getBot().ban(event.getChannel(), event.getBot().getUser(kickUser).getHostmask());
				IRCUtils.Log(IRCUtils.LOG_INFORMATION, kickUser + " (hostmask " + event.getBot().getUser(kickUser).getHostmask() + ") has been banned from the channel by " + event.getUser().getNick() + ".");
			}
		}
	}

	public KickBanHandler(MessageEvent event, boolean isBan) {
		this.event = event;
		this.isBan = isBan;
	}
	
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
}
