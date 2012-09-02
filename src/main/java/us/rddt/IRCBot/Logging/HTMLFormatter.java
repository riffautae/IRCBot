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

package us.rddt.IRCBot.Logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Formats a log entry into an HTML format, for easier reading.
 * 
 * @author Ryan Morrison
 */
public class HTMLFormatter extends Formatter {
    /**
     * Formats a provided log entry into an HTML format
     * @param rec the LogRecord entry to format
     * @return the formatted entry
     */
    public String format(LogRecord rec) {
        StringBuffer buf = new StringBuffer(1000);
        buf.append("<tr>");
        buf.append("<td>");

        if (rec.getLevel().intValue() >= Level.WARNING.intValue()) {
            buf.append("<b>");
            buf.append(rec.getLevel());
            buf.append("</b>");
        } else {
            buf.append(rec.getLevel());
        }
        buf.append("</td>");
        buf.append("<td>");
        buf.append(calcDate(rec.getMillis()));
        buf.append("</td>");
        buf.append("<td>");
        buf.append(formatMessage(rec));
        buf.append("</td>");
        buf.append("</tr>\n");
        return buf.toString();
    }
    
    /**
     * Helper method to calculate a date provided a long value
     * @param millisecs the long value in milliseconds to return to a readable date
     * @return the readable date string
     */
    private String calcDate(long millisecs) {
        SimpleDateFormat date_format = new SimpleDateFormat("MMM dd, yyyy hh:mma z");
        Date resultdate = new Date(millisecs);
        return date_format.format(resultdate);
    }

    /**
     * Returns the header to apply to the handler's output
     * @param h the handler to apply the header to
     * @return the header
     */
    public String getHead(Handler h) {
        return "<html>\n<head></head>\n<body>Log Started: " + (new Date()) + "\n<pre>\n"
                + "<table border>\n  "
                + "<tr><th>Level</th><th>Time</th><th>Message</th></tr>\n";
    }

    /**
     * Returns the footer to apply to the handler's output
     * @param h the handler to apply the footer to
     * @return the footer
     */
    public String getTail(Handler h) {
        return "</table>\n  </pre></body>\n</html>\n";
    }
}
