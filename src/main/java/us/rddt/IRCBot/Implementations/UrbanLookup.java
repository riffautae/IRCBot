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
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import us.rddt.IRCBot.IRCUtils;

/**
 * @author Ryan Morrison
 */
public class UrbanLookup {
    /*
     * Class variables
     */
    private boolean hasResult;
    private String word;
    private String definition;
    private String example;
    
    /**
     * Class constructor
     */
    public UrbanLookup() {  
    }
    
    /**
     * Class constructor
     * @param hasResult if the lookup returned a result or not
     * @param word the defined word
     * @param definition the word's definition
     * @param example an example of the word
     */
    public UrbanLookup(boolean hasResult, String word, String definition, String example) {
        this.hasResult = hasResult;
        this.word = word;
        this.definition = definition;
        this.example = example;
    }
    
    /**
     * Gets the definition of a provided word
     * @param toDefine the word to define
     * @return a new instance of the class with the definition
     * @throws IOException if the download fails
     * @throws JSONException if the JSON cannot be parsed
     */
    public static UrbanLookup getDefinition(String toDefine) throws IOException, JSONException {
        URL lookupURL = null;
        StringBuilder jsonToParse = new StringBuilder();
        String buffer;
        
        try {
            lookupURL = new URL("http://www.urbandictionary.com/iphone/search/define?term=" + toDefine);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
        
        /*
         * Opens a connection to the provided URL, and downloads the data into a temporary variable.
         */
        HttpURLConnection conn = (HttpURLConnection)lookupURL.openConnection();
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
        
        JSONObject lookupResult = new JSONObject(jsonToParse.toString());
        if(!lookupResult.getString("result_type").equals("exact")) {
            return new UrbanLookup(false, null, null, null);
        } else {
            return new UrbanLookup(true,
                    lookupResult.getJSONArray("list").getJSONObject(0).getString("word"),
                    lookupResult.getJSONArray("list").getJSONObject(0).getString("definition"),
                    lookupResult.getJSONArray("list").getJSONObject(0).getString("example"));
        }
    }

    /**
     * Returns true if the definition lookup succeeded, false if not
     * @return true if the definition lookup succeeded, false if not
     */
    public boolean hasResult() {
        return hasResult;
    }

    /**
     * Returns the defined word
     * @return the defined word
     */
    public String getWord() {
        return word;
    }

    /**
     * Returns the word's definition
     * @return the word's definition
     */
    public String getDefinition() {
        return definition;
    }

    /**
     * Returns the word's example
     * @return the word's example
     */
    public String getExample() {
        return example;
    }
}
