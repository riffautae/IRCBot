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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import us.rddt.IRCBot.Enums.LogLevels;
import us.rddt.IRCBot.Implementations.RedditLink;
import us.rddt.IRCBot.Implementations.RedditUser;
import us.rddt.IRCBot.Implementations.YouTubeLink;

/**
 * @author Ryan Morrison
 */
public class URLGrabber implements Runnable {
    /*
     * Class variables.
     */
    private MessageEvent<PircBotX> event = null;
    private URL url = null;

    // Regex pattern to match imgur links
    private static final Pattern IMGUR_LINK = Pattern.compile("http:\\/\\/(www.)?(i.)?imgur\\.com\\/.+");
    // Regex pattern to match Reddit links
    private static final Pattern REDDIT_LINK = Pattern.compile("https?:\\/\\/(www.)?reddit\\.com\\/r\\/.+\\/comments\\/.+\\/.+\\/");
    // Regex pattern to match Reddit users
    private static final Pattern REDDIT_USER = Pattern.compile("https?:\\/\\/(www.)?reddit\\.com\\/user\\/.+");
    // Regex pattern to match the HTML title tag to extract from the URL
    private static final Pattern TITLE_TAG = Pattern.compile("\\<title>(.*)\\</title>", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
    // Regex pattern to match Twitter tweets
    private static final Pattern TWITTER_TWEET = Pattern.compile("https?:\\/\\/twitter\\.com\\/(?:#!\\/)?(\\w+)\\/status(es)?\\/(\\d+)");
    // Regex pattern to match YouTube videos
    private static final Pattern YOUTUBE_VIDEO = Pattern.compile("http:\\/\\/(www.)?youtube\\.com\\/watch\\?v=.+");

    /**
     * Content-Type class definition
     */
    private static final class ContentType {
        // Regex pattern to match the character set from the Content-Type
        private static final Pattern CHARSET_HEADER = Pattern.compile("charset=([-_a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);

        // Variables
        private String charsetName;
        private String contentType;

        // Constructor for the ContentType class
        private ContentType(String headerValue) {
            // Throw an exception should the passed parameter be null
            if (headerValue == null)
                throw new IllegalArgumentException("ContentType must be constructed with a not-null headerValue");
            // Locate the index of the semicolon in the header and use the regex above to match and extract the character set
            // If a semicolon doesn't exist then the character set was never provided, so set the Content-Type appropriately
            int n = headerValue.indexOf(";");
            if (n != -1) {
                contentType = headerValue.substring(0, n);
                Matcher matcher = CHARSET_HEADER.matcher(headerValue);
                if (matcher.find())
                    charsetName = matcher.group(1);
            }
            else
                contentType = headerValue;
        }
    }

    /**
     * Extracts the character set from the Content-Type header property
     * @param contentType the Content-Type property to parse
     * @return the character set
     */
    private static Charset getCharset(ContentType contentType) {
        // Extract the character set from the character set or return null upon failure
        if (contentType != null && contentType.charsetName != null && Charset.isSupported(contentType.charsetName))
            return Charset.forName(contentType.charsetName);
        else
            return null;
    }
    /**
     * Extracts the Content-Length property from the HTTP response
     * @param conn the open HTTP connection to read the headers from
     * @return the length of the data stream
     */
    private static int getContentLengthHeader(HttpURLConnection conn) {
        // Variables
        int i = 0;
        boolean moreHeaders = true;
        // Loop through the headers until we find the Content-Length property
        // If we find it, break out, otherwise continue reading
        do {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);
            if (headerName != null && headerName.equals("Content-Length"))
                return Integer.parseInt(headerValue);
            i++;
            moreHeaders = headerName != null || headerValue != null;
        }
        while (moreHeaders);
        // If we reach this point we couldn't find the headers we need, so return 0
        return 0;
    }

    /**
     * Extracts the Content-Type property from the HTTP response
     * @param conn the open HTTP connection to read the headers from
     * @return the content type header value(s)
     */
    private static ContentType getContentTypeHeader(HttpURLConnection conn) {
        // Variables
        int i = 0;
        boolean moreHeaders = true;
        // Loop through the headers until we find the Content-Type property
        // If we find it, break out, otherwise continue reading
        do {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);
            if (headerName != null && headerName.equals("Content-Type"))
                return new ContentType(headerValue);
            i++;
            moreHeaders = headerName != null || headerValue != null;
        }
        while (moreHeaders);
        // If we reach this point we couldn't find the headers we need, so return null
        return null;
    }

    /**
     * Converts a data measurement value to a more human-readable format
     * @param bytes the length to convert into a data measurement
     * @param si whether to use the SI measurement
     * @return the formatted human-readable string
     */
    private static String humanReadableByteCount(long bytes, boolean si) {
        // Variable for the unit of measurement used
        int unit = si ? 1000 : 1024;
        // If our value is less than a kilobyte than just return the value untouched in bytes
        if (bytes < unit) return bytes + " B";
        // Otherwise, properly convert to the appropriate human-readable value and return it
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Class constructor
     */
    public URLGrabber(MessageEvent<PircBotX> event, URL url) {
        this.event = event;
        this.url = url;
    }

    /**
     * Determines if an imgur link has been posted to Reddit and return post details if it has
     * @param imgurURL the imgur URL to check against Reddit
     * @return true if the image appears on Reddit, false if it does not
     */
    private boolean checkImgurReddit(URL imgurURL) {
        // Variables
        URL appendURL = null;

        // Construct the appropriate URL to get the JSON via the Reddit API
        try {
            appendURL = new URL("http://www.reddit.com/api/info.json?url=" + imgurURL.toString());
            RedditLink link = new RedditLink();
            RedditLink bestSubmission = link.checkImgurLink(appendURL);
            if(bestSubmission != null) {
                String formattedString = "[imgur by '" + event.getUser().getNick() + "'] As spotted on Reddit: " + Colors.BOLD + bestSubmission.getTitle() + Colors.NORMAL + " (submitted by " + bestSubmission.getAuthor() + " to r/" + bestSubmission.getSubreddit() + " about " + bestSubmission.getCreatedReadableUTC() + " ago, " + bestSubmission.getScore() + " points: http://redd.it/" + bestSubmission.getId() + ")";
                if(bestSubmission.isOver18()) {
                    formattedString += (" " + Colors.BOLD + Colors.RED + "[NSFW]");
                }
                if(bestSubmission.isNSFL()) {
                    formattedString += (" " + Colors.BOLD + Colors.RED + "[NSFL]");
                }
                event.getBot().sendMessage(event.getChannel(), formattedString);
                return true;
            } else {
                return false;
            }
        } catch (MalformedURLException ex) {
            IRCUtils.Log(LogLevels.ERROR, ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            IRCUtils.Log(LogLevels.ERROR, ex.getMessage());
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Formats a friendly error message to return to a user if a lookup fails
     * @param site the website where the lookup failed
     * @param message the error message
     */
    private String formatError(String site, String message) {
        return "[" + site + " by '" + event.getUser().getNick() + "'] An error occurred while retrieving this URL. (" + IRCUtils.trimString(message, 50) + ")";
    }

    /**
     * Gets the page title from a provided URL
     * @param url the URL of the page to extract the title from
     * @return the page title
     * @throws Exception if an error occurs downloading the page
     */
    private String getPageTitle(URL url) throws Exception {
        // Connect to the server
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        // Set a proper user agent, some sites return HTTP 409 without it
        conn.setRequestProperty("User-Agent", IRCUtils.USER_AGENT);
        // Initiate the connection
        if(conn.getResponseCode() >= 400) {
            throw new IOException("Server returned response code: " + conn.getResponseCode());
        }
        // No need to check validity of the URL - it's already been proven valid at this point
        // Get the Content-Type property from the HTTP headers so we can parse accordingly
        ContentType contentType = getContentTypeHeader(conn);
        // If the document isn't HTML, return the Content-Type and Content-Length instead
        if(!contentType.contentType.equals("text/html")) {
            return "Type: " + contentType.contentType + ", length: " + humanReadableByteCount(getContentLengthHeader(conn), true);
        }
        else {
            // Get the character set or use the default accordingly
            Charset charset = getCharset(contentType);
            if(charset == null) charset = Charset.defaultCharset();
            // Create and prepare our streams for reading from the web server
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
            // More variables
            int n = 0, totalRead = 0;
            char[] buf = new char[1024];
            StringBuilder content = new StringBuilder();

            // Read the page from the web server into the buffer up to 8192 bytes and create a string from it
            // The first 8192 bytes should be enough to fetch the title, while saving us time and bandwidth
            while(totalRead < 8192 && (n = reader.read(buf, 0, buf.length)) != -1) {
                content.append(buf, 0, n);
                totalRead += n;
            }
            // Close the BufferedReader
            reader.close();
            // Disconnect from the web server
            conn.disconnect();

            // Use the Regex defined earlier to match and extract the title tag
            // If the title tag can't be matched, it likely wasn't part of the first 8192 bytes
            Matcher matcher = TITLE_TAG.matcher(content);
            if (matcher.find()) {
                // Properly escape any HTML entities present in the title
                return Colors.BOLD + IRCUtils.escapeHTMLEntities((matcher.group(1).replaceAll("[\\s\\<>]+", " ").trim()));
            }
            else
                return "Title not found or not within first 8192 bytes of page, aborting.";
        }
    }

    /**
     * Prints the title of a Reddit submissions or information about a user depending on the URL provided
     * @param redditURL the reddit URL to extract the data from
     * @param isUser is the URL of a user's page
     */
    private void returnReddit(URL redditURL, boolean isUser) {
        // Variables
        URL appendURL = null;

        // Construct the appropriate URL to get the JSON via the Reddit API
        try {
            if(isUser) {
                appendURL = new URL(redditURL.toString() + "/about.json");
                RedditUser user = RedditUser.getUser(appendURL);
                String formattedString = "[Reddit by '" + event.getUser().getNick() + "'] " + Colors.BOLD + user.getName() + Colors.NORMAL + ": " + user.getLinkKarma() + " link karma, " + user.getCommentKarma() + " comment karma, user since " + user.getReadableCreated();
                if(user.isGold()) {
                    formattedString += " [reddit gold]";
                }
                event.getBot().sendMessage(event.getChannel(), formattedString);
                return;
            }
            else {
                appendURL = new URL(redditURL.toString() + "/.json");
                RedditLink link = RedditLink.getLink(appendURL);
                String formattedString = "[Reddit by '" + event.getUser().getNick() + "'] " + Colors.BOLD + link.getTitle() + Colors.NORMAL + " (submitted by " + link.getAuthor() + " to r/" + link.getSubreddit() + " about " +  link.getCreatedReadableUTC() + " ago, " + link.getScore() + " points)";
                if(link.isOver18()) {
                    formattedString += (" " + Colors.BOLD + Colors.RED + "[NSFW]");
                }
                if(link.isNSFL()) {
                    formattedString += (" " + Colors.BOLD + Colors.RED + "[NSFL]");
                }
                event.getBot().sendMessage(event.getChannel(), formattedString);
                return;
            }
        } catch (MalformedURLException ex) {
            IRCUtils.Log(LogLevels.ERROR, ex.getMessage());
            ex.printStackTrace();
            return;
        } catch (Exception ex) {
            event.getBot().sendMessage(event.getChannel(), formatError("Reddit", ex.getMessage()));
            IRCUtils.Log(LogLevels.ERROR, ex.getMessage());
            ex.printStackTrace();
            return;
        }
    }

    /**
     * Prints the content of a provided tweet to a specified channel
     * @param tweetID the ID value of the tweet to print
     */
    private void returnTweet(long tweetID) {
        try {
            // Get the Tweet and send it back to the channel
            Twitter twitter = new TwitterFactory().getInstance();
            Status status = twitter.showStatus(tweetID);
            event.getBot().sendMessage(event.getChannel(), "[Tweet by '" + event.getUser().getNick() + "'] " + Colors.BOLD + "@" + status.getUser().getScreenName() + Colors.NORMAL + ": " + status.getText());
        } catch (TwitterException te) {
            event.getBot().sendMessage(event.getChannel(), formatError("Twitter", te.getMessage()));
            IRCUtils.Log(LogLevels.ERROR, te.getMessage());
            te.printStackTrace();
        }
    }

    /**
     * Prints the title and duration of a YouTube video to a specified channel
     * @param youtubeURL the URL to process
     */
    private void returnYouTubeVideo(URL youtubeURL) {
        URL appendURL = null;

        // Construct the URL to read the JSON data from
        try {
            appendURL = new URL("http://gdata.youtube.com/feeds/api/videos?q=" + url.toString().split("=")[1] + "&v=2&alt=jsonc");
            YouTubeLink link = YouTubeLink.getLink(appendURL);
            event.getBot().sendMessage(event.getChannel(), "[YouTube by '" + event.getUser().getNick() + "'] " + Colors.BOLD + link.getTitle() + Colors.NORMAL + " (" + link.getReadableDuration() + ")");
            return;
        } catch (MalformedURLException ex) {
            IRCUtils.Log(LogLevels.ERROR, ex.getMessage());
            ex.printStackTrace();
            return;
        } catch (Exception ex) {
            event.getBot().sendMessage(event.getChannel(), formatError("YouTube", event.getMessage()));
            IRCUtils.Log(LogLevels.ERROR, ex.getMessage());
            ex.printStackTrace();
            return;
        }
    }

    /**
     * Method that executes upon thread start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        // Run the URL through each regex pattern and parse accordingly
        Matcher urlMatcher = TWITTER_TWEET.matcher(url.toString());
        if(urlMatcher.find()) {
            returnTweet(Long.parseLong(url.toString().substring(url.toString().lastIndexOf("/")).replaceAll("/", "")));
            return;
        }
        urlMatcher = REDDIT_LINK.matcher(url.toString());
        if(urlMatcher.find()) {
            try {
                returnReddit(new URL(urlMatcher.group()), false);
            } catch (MalformedURLException ex) {
                return;
            }
            return;
        }
        urlMatcher = REDDIT_USER.matcher(url.toString());
        if(urlMatcher.find()) {
            returnReddit(url, true);
            return;
        }
        urlMatcher = IMGUR_LINK.matcher(url.toString());
        if(urlMatcher.find()) {
            if(checkImgurReddit(url)) return;
        }
        urlMatcher = YOUTUBE_VIDEO.matcher(url.toString());
        if(urlMatcher.find()) {
            returnYouTubeVideo(url);
            return;
        }
        // If none of the regex patterns matched, then get the page title/length
        try {
            event.getBot().sendMessage(event.getChannel(), ("[URL by '" + event.getUser().getNick() + "'] " + getPageTitle(url)));
        } catch (Exception ex) {
            IRCUtils.Log(LogLevels.ERROR, ex.getMessage());
            ex.printStackTrace();
            event.getBot().sendMessage(event.getChannel(), formatError("URL", ex.getMessage()));
            return;
        }
    }
    
    /*
     * Static block to ensure that HTTPS connections don't bother validating certificate chains
     * Only executes on the initial class creation, doesn't run in every thread 
     */
    static {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        try {
            // Ensure that HTTPS connections use our custom trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
