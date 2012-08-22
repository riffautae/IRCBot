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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * @author Ryan Morrison
 */
public class IRCUtils {
    /* 
     * Sun property pointing the main class and its arguments. 
     * Might not be defined on non Hotspot VM implementations.
     */
    public static final String SUN_JAVA_COMMAND = "sun.java.command";

    /**
     * Returns a properly escaped string value
     * @param toEscape the string to escape
     * @return the escaped string
     */
    public static String escapeHTMLEntities(String toEscape) {
        new StringEscapeUtils();
        return StringEscapeUtils.unescapeHtml4(toEscape);
    }
    
    /**
     * Restart the current Java application
     * @throws URISyntaxException 
     */
    public static void restartApplication() throws IOException, URISyntaxException
    {
      final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
      final File currentJar = new File(IRCBot.class.getProtectionDomain().getCodeSource().getLocation().toURI());

      /* is it a jar file? */
      if(!currentJar.getName().endsWith(".jar"))
        return;

      /* Build command: java -jar application.jar */
      final ArrayList<String> command = new ArrayList<String>();
      command.add(javaBin);
      command.add("-jar");
      command.add(currentJar.getPath());

      final ProcessBuilder builder = new ProcessBuilder(command);
      builder.start();
      System.exit(0);
    }

    /**
     * Converts a time format in long form to a human-readable minute value
     * @param time the time to be converted
     * @return the human-readable minute value
     */
    public static String toReadableMinutes(long time) {
        if(time > 3600) {
            return String.format("%d:%02d:%02d", time/3600, (time % 3600)/60, (time % 60));
        } else {
            return String.format("%d:%02d", (time % 3600)/60, (time % 60));
        }
    }

    /**
     * Converts date formats to human-readable strings
     * @param date the date to be converted
     * @param countingDown true if we are counting down to a date, false if we are determining history
     * @param showMoment true to show seconds as "a moment", false to show the number of seconds
     * @return the human-readable string
     */
    public static String toReadableTime(Date date, boolean countingDown, boolean showMoment) {
        // Calculate the difference in seconds between the time the user left and now
        long diffInSeconds;
        if(countingDown) diffInSeconds = (date.getTime() - System.currentTimeMillis()) / 1000;
        else diffInSeconds = (System.currentTimeMillis() - date.getTime()) / 1000;

        // Calculate the appropriate day/hour/minute/seconds ago values and insert them into a long array
        long diff[] = new long[] { 0, 0, 0, 0 };
        diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
        diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
        diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
        diff[0] = (diffInSeconds = (diffInSeconds / 24));

        // Build the readable format string
        if(diff[0] != 0) return String.format("%d day%s", diff[0], diff[0] > 1 ? "s" : "");
        if(diff[1] != 0) return String.format("%s%s hour%s", diff[1] > 1 ? "" : "an", diff[1] > 1 ? String.valueOf(diff[1]) : "", diff[1] > 1 ? "s" : "");
        if(diff[2] != 0) return String.format("%d minute%s", diff[2], diff[2] > 1 ? "s" : "");
        if(diff[3] != 0) {
            if(showMoment) return "a moment";
            else return String.format("%d second%s", diff[3], diff[3] > 1 ? "s" : "");
        }
        else return "unknown";
    }

    /**
     * Trims strings greater than a provided length
     * @param toTrim the string to be trimmed, if necessary
     * @param maxLength the maximum length the string can be
     * @return the trimmed string
     */
    public static String trimString(String toTrim, int maxLength) {
        if(toTrim.length() > maxLength) {
            return toTrim.substring(0, maxLength) + "...";
        } else {
            return toTrim;
        }
    }
}
