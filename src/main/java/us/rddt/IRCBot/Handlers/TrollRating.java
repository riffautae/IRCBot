package us.rddt.IRCBot.Handlers;

import java.util.Random;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

public class TrollRating implements Runnable {
    private MessageEvent<PircBotX> event;
    
    public TrollRating(MessageEvent<PircBotX> event) {
        this.event = event;
    }
    
    public void run() {
        Random generator = new Random();
		String[] ratings = { "poor", "weak", "mild", "sub-par", "moderate", "solid", "successful", "good", "great", "excellent" };
        int randomNum = generator.nextInt(10);
        event.getBot().sendMessage(event.getChannel(), randomNum + "/10, " + ratings[randomNum] + " troll");
    }
}
