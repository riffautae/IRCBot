package us.rddt.IRCBot.Implementations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import us.rddt.IRCBot.Configuration;

public class Converter {
    /*
     * Variables
     */
    private String lhs;
    private String rhs;
    private String error;
    private boolean icc;
    
    /**
     * Class constructor
     */
    public Converter() {  
    }
    
    /**
     * Class constructor
     * @param lhs the left hand side of the equation (input)
     * @param rhs the right hand side of the equation (output)
     * @param error the error string (if applicable)
     * @param icc
     */
    public Converter(String lhs, String rhs, String error, boolean icc) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.error = error;
        this.icc = icc;
    }
    
    /**
     * Gets information about a provided link to a Reddit user page.
     * @param link the link to the user page
     * @return a new instance of the class with the user's details
     * @throws IOException if the download fails
     * @throws JSONException if the JSON cannot be parsed
     */
    public static Converter convert(String lhs) throws IOException, JSONException {
        /*
         * Variables
         */
        StringBuilder jsonToParse = new StringBuilder();
        String buffer;
        URL link = new URL("http://www.google.com/ig/calculator?hl=en&q=" + lhs.replace(" ", "%20"));

        /*
         * Opens a connection to the Google API, and downloads the data into a temporary variable.
         */
        HttpURLConnection conn = (HttpURLConnection)link.openConnection();
        conn.setRequestProperty("User-Agent", Configuration.getUserAgent());
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
        JSONObject result = new JSONObject(jsonToParse.toString());
        return new Converter(result.getString("lhs"), result.getString("rhs"), result.getString("error"), result.getBoolean("icc"));
    }

    /**
     * Returns the left hand side of the equation (input)
     * @return the left hand side of the equation (input)
     */
    public String getLhs() {
        return lhs;
    }

    /**
     * Returns the right hand side of the equation (output)
     * @return the right hand side of the equation (output)
     */
    public String getRhs() {
        return rhs;
    }
    
    /**
     * Returns the error string (if applicable)
     * @return the error string (if applicable)
     */
    public String getError() {
        return error;
    }

    /**
     * Returns the value of icc
     * @return the value of icc
     */
    public boolean isIcc() {
        return icc;
    }
}
