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

package us.rddt.IRCBot.Implementations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.NoSuchElementException;

import org.json.JSONException;
import org.json.JSONObject;

import us.rddt.IRCBot.IRCUtils;

/**
 * @author Ryan Morrison
 */
public class YouTubeLink {
    /*
     * Variables
     */
    private String title;
    private long duration;

    /**
     * Class constructor
     */
    public YouTubeLink() {
    }

    /**
     * Gets information about a provided link to a YouTube video
     * @param link the link to the user page
     * @throws IOException if the download fails
     * @throws JSONException if the JSON cannot be parsed
     */
    public void getLink(URL link) throws IOException, JSONException {
        /*
         * Variables
         */
        StringBuilder jsonToParse = new StringBuilder();
        String buffer;

        /*
         * Opens a connection to the provided URL, and downloads the data into a temporary variable.
         */
        HttpURLConnection conn = (HttpURLConnection)link.openConnection();
        conn.setRequestProperty("User-Agent", IRCUtils.USER_AGENT);
        if(conn.getResponseCode() >= 400) {
            throw new IOException("Server returned response code: " + conn.getResponseCode());
        }

        BufferedReader buf = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while((buffer = buf.readLine()) != null) {
            jsonToParse.append(buffer);
        }

        /*
         * Disconnect from the server.
         */
        conn.disconnect();

        /*
         * Parse the JSON data.
         */
        JSONObject parsedArray = new JSONObject(jsonToParse.toString());
        if(parsedArray.getJSONObject("data").getInt("totalItems") > 0) {
            JSONObject youtubeLink = parsedArray.getJSONObject("data").getJSONArray("items").getJSONObject(0);
            this.title = IRCUtils.escapeHTMLEntities(youtubeLink.getString("title"));
            this.duration = youtubeLink.getLong("duration");
        } else {
            throw new NoSuchElementException("YouTube video ID invalid or video is private.");
        }
    }

    /**
     * Returns the video's title
     * @return the video's title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the video's duration
     * @return the video's duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Returns the video's duration in a readable string format
     * @return the video's duration in a readable string format
     */
    public String getReadableDuration() {
        return IRCUtils.toReadableMinutes(getDuration());
    }
}
