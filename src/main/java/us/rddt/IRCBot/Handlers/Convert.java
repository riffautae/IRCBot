package us.rddt.IRCBot.Handlers;

import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import us.rddt.IRCBot.Configuration;
import us.rddt.IRCBot.IRCUtils;
import us.rddt.IRCBot.Implementations.Converter;

public class Convert implements Runnable {
    /*
     * Class variables
     */
    private MessageEvent<PircBotX> event;
    
    /**
     * Class constructor
     * @param event the MessageEvent that triggered this class
     */
    public Convert(MessageEvent<PircBotX> event) {
        this.event = event;
    }
    
    /**
     * Method that executes upon thread-start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            // Perform the conversion and return the result to the user, if an error hasn't occurred
            Converter converted = Converter.convert(event.getMessage().substring(9));
            if(converted.getError().isEmpty()) {
                event.respond(converted.getLhs() + " is " + converted.getRhs());
            } else {
                event.respond("Your conversion request is invalid.");
            }
        } catch (Exception ex) {
            Configuration.getLogger().write(Level.SEVERE, IRCUtils.getStackTraceString(ex));
        }
    }
}
