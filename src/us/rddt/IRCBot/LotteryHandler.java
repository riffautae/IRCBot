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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LotteryHandler implements Runnable {
	// Variables
	private MessageEvent event;

	// We need this to be accessible from other threads, so we make it static and volatile
	private static volatile Map<String, Date> lotteryPlayers = Collections.synchronizedMap(new HashMap<String, Date>());

	public void run() {
		// If the user submitted an actual guess
		if(event.getMessage() != "") {
			// If the user hasn't guessed previously or their 30-minute window is up
			if(lotteryPlayers.get(event.getUser().getHostmask()) == null || (((lotteryPlayers.get(event.getUser().getHostmask()).getTime() / 1000) - new Date().getTime() / 1000) < 0)) {
				// Store that they guessed
				lotteryPlayers.put(event.getUser().getHostmask(), new Date(new Date().getTime() + (43200 * 1000)));
				// Generate the winning number
				Random generator = new Random();
				int lotteryNumber = generator.nextInt(10) + 1;
				// Ensure the guess is a valid one
				int guessedNumber = 0;
				try {
					guessedNumber = Integer.parseInt(event.getMessage().substring(9).replaceAll("^\\s+", "").replaceAll("\\s+$", ""));
				} catch (NumberFormatException ex) {
					event.getBot().sendMessage(event.getUser(), "Your guess was not a number! You can try again in 12 hours.");
					return;
				} catch (IndexOutOfBoundsException ex) {
					return;
				}
				// Did they win?
				if(guessedNumber == lotteryNumber) {
					// They did! Crown them and let everyone know!
					new Thread(new KingHandler(event)).start();
					event.respond("YOU WON! ALL HAIL KING " + event.getUser().getNick() + "!");
					IRCUtils.Log(IRCUtils.LOG_INFORMATION, "User " + event.getUser().getNick() + " has been crowned king.");
				} else {
					// Not this time. (PM the user as to avoid channel flooding)
					event.respond("Sorry, you lost! You can try again in 12 hours. (Guessed " + event.getMessage().substring(9) + ", correct " + lotteryNumber + ")");
				}
			} else {
				event.respond("You still need to wait " + IRCUtils.toReadableTime(lotteryPlayers.get(event.getUser().getHostmask()), true) + " until you can play again.");
			}
		} 
	}

	// Class constructor
	public LotteryHandler(MessageEvent event) {
		this.event = event;
	}
}