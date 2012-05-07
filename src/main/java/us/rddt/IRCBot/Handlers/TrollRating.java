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
        String message = new String();
        int randomNum = generator.nextInt(10);
        
        switch(randomNum) {
        case 1:
            message = randomNum + "/10, poor troll";
            break;
        case 2:
            message = randomNum + "/10, weak troll";
            break;
        case 3:
            message = randomNum + "/10, mild troll";
            break;
        case 4:
            message = randomNum + "/10, sub-par troll";
            break;
        case 5:
            message = randomNum + "/10, moderate troll";
            break;
        case 6:
            message = randomNum + "/10, solid troll";
            break;
        case 7:
            message = randomNum + "/10, successful troll";
            break;
        case 8:
            message = randomNum + "/10, good troll";
            break;
        case 9:
            message = randomNum + "/10, great troll";
            break;
        case 10:
            message = randomNum + "/10, excellent troll";
            break;
        }
        
        event.getBot().sendMessage(event.getChannel(), message);
    }
}
